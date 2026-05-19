import request, { type ApiResponse } from './request'

export interface UploadFileResult {
  url: string
  storedPath: string
  originalFilename: string
}

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<UploadFileResult>>('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
