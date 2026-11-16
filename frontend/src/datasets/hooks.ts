import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { DataFile, TableSchema } from '../api/types';
import { isSettled } from './fileStatus';

/** Versions are fetched in one page; the sidebar has no paging. */
export const VERSION_PAGE_SIZE = 100;
const POLL_MS = 3000;

export const keys = {
  dataSource: (id: string) => ['data-source', id] as const,
  dataSets: (dataSourceId: string) => ['datasets', 'data-source', dataSourceId] as const,
  files: (dataSetId: string) => ['datasets', 'files', dataSetId] as const,
  preview: (fileId: string, lines: number) => ['datasets', 'preview', fileId, lines] as const,
};

export function useDataSources() {
  return useQuery({ queryKey: ['data-sources'], queryFn: api.listDataSources });
}

export function useDataSource(id: string) {
  return useQuery({ queryKey: keys.dataSource(id), queryFn: () => api.getDataSource(id) });
}

export function useDataSets(dataSourceId: string) {
  return useQuery({
    queryKey: keys.dataSets(dataSourceId),
    queryFn: () => api.listDataSets(dataSourceId, VERSION_PAGE_SIZE),
    select: (page) => [...page.content].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    refetchInterval: (query) =>
      query.state.data?.content.some((d) => d.lockStatus === 'LOCKING') ? POLL_MS : false,
  });
}

export function useFiles(dataSetId: string | null, keepPolling: boolean) {
  return useQuery({
    queryKey: keys.files(dataSetId ?? 'none'),
    queryFn: () => api.listFiles(dataSetId!),
    enabled: dataSetId !== null,
    refetchInterval: (query) =>
      keepPolling || query.state.data?.some((f: DataFile) => !isSettled(f)) ? POLL_MS : false,
  });
}

export function usePreview(fileId: string, lines: number, enabled: boolean) {
  return useQuery({
    queryKey: keys.preview(fileId, lines),
    queryFn: () => api.preview(fileId, lines),
    enabled,
    retry: false,
  });
}

function useInvalidate() {
  const client = useQueryClient();
  return () => {
    client.invalidateQueries({ queryKey: ['datasets'] });
    client.invalidateQueries({ queryKey: ['data-source'] });
  };
}

export function useDataSetMutations(dataSourceId: string) {
  const invalidate = useInvalidate();
  return {
    create: useMutation({ mutationFn: (name: string) => api.createDataSet(dataSourceId, name), onSuccess: invalidate }),
    rename: useMutation({
      mutationFn: ({ id, name }: { id: string; name: string }) => api.renameDataSet(id, name),
      onSuccess: invalidate,
    }),
    remove: useMutation({ mutationFn: api.deleteDataSet, onSuccess: invalidate }),
    lock: useMutation({ mutationFn: api.lockDataSet, onSettled: invalidate }),
    unlock: useMutation({ mutationFn: api.unlockDataSet, onSettled: invalidate }),
  };
}

export function useFileMutations() {
  const invalidate = useInvalidate();
  return {
    remove: useMutation({ mutationFn: api.deleteFile, onSuccess: invalidate }),
    infer: useMutation({ mutationFn: api.inferSchema, onSettled: invalidate }),
    updateSchema: useMutation({
      mutationFn: ({ id, schema }: { id: string; schema: TableSchema[] }) => api.updateSchema(id, schema),
      onSuccess: invalidate,
    }),
  };
}
