import sqlite3
db = r"d:/source_code/workflow/flow-engine/flow_engine.db"
c = sqlite3.connect(db)
for r in c.execute("SELECT model_key, model_name, version, status, substr(model_json,1,80) FROM wf_data_model WHERE model_key IN ('hazardous','retain')"):
    print(r)
c.close()
