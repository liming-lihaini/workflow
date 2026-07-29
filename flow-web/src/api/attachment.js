import request from './request'

/** 上传附件（multipart），返回 { name, path, size } */
export function uploadAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/attachments/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 下载附件：携带 token 拉取 blob 后触发浏览器保存 */
export async function downloadAttachment(path, name) {
  const res = await request({
    url: '/attachments/download',
    method: 'get',
    params: { path, name },
    responseType: 'blob',
    timeout: 60000
  })
  const blob = res instanceof Blob ? res : new Blob([res])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name || 'attachment'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
