// test_role_fix.js - 验证角色→用户解析修复
const BASE = 'http://localhost:8080/api/v1';

async function api(path, method = 'GET', body = null, token = null) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (token) opts.headers['X-Auth-Token'] = token;
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(BASE + path, opts);
  const json = await res.json();
  if (json.code !== 0) throw new Error(`API ${path} error: ${json.code} ${json.message}`);
  return json.data;
}

(async () => {
  let passed = 0, failed = 0;

  function ok(name, detail = '') {
    passed++;
    console.log(`  PASS: ${name}${detail ? ' - ' + detail : ''}`);
  }
  function fail(name, err) {
    failed++;
    console.log(`  FAIL: ${name} - ${err}`);
  }

  try {
    // 1. Login
    console.log('TC-01: Login');
    const loginRes = await fetch(BASE + '/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'songjiang', password: '123456' })
    });
    const loginData = await loginRes.json();
    if (loginData.code !== 0) throw new Error('Login failed');
    const token = loginData.data.token;
    ok('TC-01', 'songjiang logged in');

    // 2. Check role exists
    console.log('TC-02: Check HR role exists');
    const roles = await api('/system/roles', 'GET', null, token);
    const hrRole = roles.find(r => r.roleKey && r.roleKey.includes('\u4eba\u529b\u8d44\u6e90'));
    if (!hrRole) {
      fail('TC-02', 'No HR role found');
      throw new Error('No HR role');
    }
    ok('TC-02', `HR role: id=${hrRole.id}, key=${hrRole.roleKey}`);

    // 3. Check role has users
    console.log('TC-03: Check role users');
    const roleKey = hrRole.roleKey;
    const roleUsers = await api(`/system/roles/${hrRole.id}/users`, 'GET', null, token);
    if (roleUsers.length === 0) {
      fail('TC-03', 'No users in role');
    } else {
      ok('TC-03', `${roleUsers.length} users in role: ${roleUsers.map(u => u.userId).join(',')}`);
    }

    // 4. Create process definition with role-based approval
    console.log('TC-04: Create process with role assignee');
    const procKey = 'ROLE_FIX_' + Date.now();
    const procDef = {
      processKey: procKey,
      processName: '\u89d2\u8272\u6307\u6d3e\u6d4b\u8bd5\u6d41\u7a0b',
      category: 'leave',
      processJson: JSON.stringify({
        nodes: [
          { id: '1', type: 'start', name: '\u5f00\u59cb', position: { x: 100, y: 200 } },
          {
            id: '2', type: 'userTask', name: '\u89d2\u8272\u5ba1\u6279',
            position: { x: 300, y: 200 },
            assigneeType: 'role',
            assignee: roleKey,
            candidateUsers: roleKey
          },
          { id: '3', type: 'end', name: '\u7ed3\u675f', position: { x: 500, y: 200 } }
        ],
        edges: [
          { id: 'e1', source: '1', target: '2' },
          { id: 'e2', source: '2', target: '3' }
        ]
      })
    };
    const createdDef = await api('/process/definitions', 'POST', procDef, token);
    ok('TC-04', `Created def id=${createdDef.id}, key=${procKey}`);

    // 5. Deploy the process
    console.log('TC-05: Deploy process');
    await api(`/process/definitions/${createdDef.id}/deploy`, 'POST', null, token);
    ok('TC-05', 'Deployed');

    // 6. Start process instance
    console.log('TC-06: Start process instance');
    const startRes = await api('/process/instances', 'POST', {
      processKey: procKey,
      startUser: 'songjiang',
      variables: { reason: 'test role fix' }
    }, token);
    ok('TC-06', `Instance id=${startRes.id}`);

    // 7. Check task assignee - should be actual users, not role name
    console.log('TC-07: Check task assignee is resolved to users');
    await new Promise(r => setTimeout(r, 1000));
    const tasks = await api(`/tasks/instance/${startRes.id}`, 'GET', null, token);
    if (tasks.length === 0) {
      fail('TC-07', 'No tasks created');
    } else {
      const task = tasks[0];
      console.log(`    Task: assignee=${task.assignee}, candidateUsers=${task.candidateUsers}, status=${task.status}`);
      if (task.assignee === roleKey) {
        fail('TC-07', `Assignee is still role name "${roleKey}" - NOT resolved!`);
      } else if (task.assignee && task.assignee !== roleKey) {
        ok('TC-07', `Assignee resolved to user: ${task.assignee}`);
      } else if (task.candidateUsers && task.candidateUsers !== roleKey) {
        ok('TC-07', `CandidateUsers resolved: ${task.candidateUsers}`);
      } else {
        fail('TC-07', `Neither assignee nor candidateUsers resolved`);
      }
    }

    // 8. Check todo list for resolved users
    console.log('TC-08: Check todo list for role users');
    const roleUsernames = [];
    for (const ru of roleUsers) {
      try {
        const userRes = await api(`/system/users/${ru.userId}`, 'GET', null, token);
        if (userRes && userRes.username) roleUsernames.push(userRes.username);
      } catch (e) { /* skip */ }
    }
    console.log(`    Role user usernames: ${roleUsernames.join(',')}`);

    let todoFound = false;
    for (const username of roleUsernames) {
      try {
        const todos = await api(`/tasks/todo?userId=${username}`, 'GET', null, token);
        const found = todos.some(t => t.processInstanceId === startRes.id);
        if (found) {
          ok('TC-08', `User "${username}" sees the task in todo`);
          todoFound = true;
          break;
        }
      } catch (e) { /* skip */ }
    }
    if (!todoFound) {
      const assignee = tasks[0]?.assignee;
      if (assignee) {
        const todos = await api(`/tasks/todo?userId=${assignee}`, 'GET', null, token);
        const found = todos.some(t => t.processInstanceId === startRes.id);
        if (found) ok('TC-08', `Assignee "${assignee}" sees the task in todo`);
        else fail('TC-08', `No role user sees the task`);
      } else fail('TC-08', 'No assignee and no role users found');
    }

    // Summary
    console.log(`\n========================================`);
    console.log(`Results: ${passed} passed, ${failed} failed, ${passed + failed} total`);
    console.log(`========================================`);

  } catch (e) {
    fail('Test', e.message);
    console.log(`\nFATAL ERROR: ${e.message}`);
    console.log(e.stack);
  }

  process.exit(failed > 0 ? 1 : 0);
})();
