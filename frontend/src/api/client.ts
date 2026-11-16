import type { DataFile, DataSet, DataSource, Page, Preview, TableSchema } from './types';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    let detail = response.statusText;
    try {
      detail = (await response.json()).detail ?? detail;
    } catch {
      // not JSON
    }
    throw new ApiError(response.status, detail);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  listDataSources: () => request<DataSource[]>('GET', '/api/data-sources'),
  getDataSource: (id: string) => request<DataSource>('GET', `/api/data-sources/${id}`),
  listDataSets: (dataSourceId: string, size: number) =>
    request<Page<DataSet>>('GET', `/api/data-sources/${dataSourceId}/data-sets?page=0&size=${size}&sort=createdAt,desc`),
  createDataSet: (dataSourceId: string, name: string) =>
    request<DataSet>('POST', '/api/data-sets', { dataSourceId, name }),
  renameDataSet: (id: string, name: string) => request<DataSet>('PATCH', `/api/data-sets/${id}`, { name }),
  deleteDataSet: (id: string) => request<void>('DELETE', `/api/data-sets/${id}`),
  lockDataSet: (id: string) => request<DataSet>('POST', `/api/data-sets/${id}/lock`),
  unlockDataSet: (id: string) => request<DataSet>('POST', `/api/data-sets/${id}/unlock`),
  listFiles: (dataSetId: string) => request<DataFile[]>('GET', `/api/data-sets/${dataSetId}/data-files`),
  inferSchema: (ids: string[]) => request<DataFile[]>('POST', '/api/data-files/infer-schema', ids),
  updateSchema: (id: string, schemaData: TableSchema[]) =>
    request<DataFile>('PUT', `/api/data-files/${id}/schema`, { schemaData }),
  preview: (id: string, lines: number) => request<Preview>('GET', `/api/data-files/${id}/preview?lines=${lines}`),
  deleteFile: (id: string) => request<void>('DELETE', `/api/data-files/${id}`),
  downloadUrl: (id: string) => `/api/data-files/${id}/download`,
};
