import { useCallback, useEffect, useRef, useState } from 'react';
import { UploadError, uploadFile, type UploadHandle } from '../../api/upload';
import { DONE_ROW_MS, MAX_PARALLEL, errorKind, isActive, type UploadItem } from './uploadModel';

let counter = 0;

/** A queue of uploads into one version, a few at a time. */
export function useUploads(dataSetId: string, onUploaded: (fileId: string) => void) {
  const [items, setItems] = useState<UploadItem[]>([]);
  const handles = useRef(new Map<string, UploadHandle>());
  const onUploadedRef = useRef(onUploaded);
  onUploadedRef.current = onUploaded;

  const update = (key: string, patch: Partial<UploadItem>) =>
    setItems((all) => all.map((i) => (i.key === key ? { ...i, ...patch } : i)));

  const start = useCallback(
    (item: UploadItem) => {
      update(item.key, { phase: 'UPLOADING', startedAt: Date.now(), sent: 0, error: null });
      const handle = uploadFile(dataSetId, item.file, (sent) =>
        update(item.key, { sent, phase: sent >= item.file.size ? 'PROCESSING' : 'UPLOADING' }),
      );
      handles.current.set(item.key, handle);
      handle.promise
        .then((created) => {
          update(item.key, { phase: 'DONE', sent: item.file.size });
          onUploadedRef.current(created.id);
          setTimeout(() => setItems((all) => all.filter((i) => i.key !== item.key)), DONE_ROW_MS);
        })
        .catch((e: UploadError) => {
          if (e.status === -1) return; // cancelled; the row is already gone
          update(item.key, { phase: 'FAILED', error: errorKind(e.status) });
        })
        .finally(() => handles.current.delete(item.key));
    },
    [dataSetId],
  );

  // Start queued uploads while there is room.
  useEffect(() => {
    const running = items.filter((i) => i.phase === 'UPLOADING' || i.phase === 'PROCESSING').length;
    items
      .filter((i) => i.phase === 'QUEUED' && !handles.current.has(i.key))
      .slice(0, Math.max(0, MAX_PARALLEL - running))
      .forEach(start);
  }, [items, start]);

  // Leaving the version aborts everything.
  useEffect(() => {
    const current = handles.current;
    return () => current.forEach((h) => h.abort());
  }, []);

  const add = (files: File[]) =>
    setItems((all) => [
      ...all,
      ...files.map((file) => ({
        key: `upload-${++counter}`,
        file,
        phase: 'QUEUED' as const,
        sent: 0,
        startedAt: null,
        error: null,
      })),
    ]);

  const cancel = (key: string) => {
    handles.current.get(key)?.abort();
    setItems((all) => all.filter((i) => i.key !== key));
  };

  const cancelAll = () => {
    handles.current.forEach((h) => h.abort());
    setItems((all) => all.filter((i) => !isActive(i)));
  };

  const retry = (key: string) => update(key, { phase: 'QUEUED', sent: 0, error: null });

  const dismiss = (key: string) => setItems((all) => all.filter((i) => i.key !== key));

  return { items, add, cancel, cancelAll, retry, dismiss };
}
