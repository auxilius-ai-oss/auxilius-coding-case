import type { DataFile } from './types';

export interface UploadHandle {
  promise: Promise<DataFile>;
  abort: () => void;
}

export class UploadError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/** PUTs one file as the raw body, reporting progress. XHR because fetch has no upload progress. */
export function uploadFile(dataSetId: string, file: File, onProgress: (sent: number) => void): UploadHandle {
  const xhr = new XMLHttpRequest();
  const promise = new Promise<DataFile>((resolve, reject) => {
    xhr.open('PUT', `/api/data-sets/${dataSetId}/data-files/${encodeURIComponent(file.name)}`);
    xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');
    xhr.upload.onprogress = (e) => onProgress(e.loaded);
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(JSON.parse(xhr.responseText));
      } else {
        let detail = xhr.statusText;
        try {
          detail = JSON.parse(xhr.responseText).detail ?? detail;
        } catch {
          // not JSON
        }
        reject(new UploadError(xhr.status, detail));
      }
    };
    xhr.onerror = () => reject(new UploadError(0, 'Network error'));
    xhr.onabort = () => reject(new UploadError(-1, 'Cancelled'));
    xhr.send(file);
  });
  return { promise, abort: () => xhr.abort() };
}
