export type UploadPhase = 'QUEUED' | 'UPLOADING' | 'PROCESSING' | 'DONE' | 'FAILED';
export type UploadErrorKind = 'DUPLICATE' | 'TOO_LARGE' | 'INTERRUPTED' | 'NETWORK' | 'FAILED';

export interface UploadItem {
  key: string;
  file: File;
  phase: UploadPhase;
  sent: number;
  startedAt: number | null;
  error: UploadErrorKind | null;
}

/** Cancelling asks first once this much has been sent. */
export const CONFIRM_CANCEL_BYTES = 2 * 1024 * 1024;
export const MAX_PARALLEL = 3;
export const DONE_ROW_MS = 3000;

export function errorKind(status: number): UploadErrorKind {
  if (status === 409) return 'DUPLICATE';
  if (status === 413) return 'TOO_LARGE';
  if (status === 0) return 'NETWORK';
  if (status === 503) return 'INTERRUPTED';
  return 'FAILED';
}

export const ERROR_COPY: Record<UploadErrorKind, { title: string; detail: string; retry: boolean }> = {
  DUPLICATE: {
    title: 'A file with this name already exists',
    detail: 'Delete the existing file first, or rename this one.',
    retry: false,
  },
  TOO_LARGE: { title: 'File too large', detail: 'Files can be at most 4 MB.', retry: false },
  INTERRUPTED: { title: 'Upload interrupted', detail: 'The server stopped receiving the file. Try again.', retry: true },
  NETWORK: { title: 'Connection lost', detail: 'Check your connection and try again.', retry: true },
  FAILED: { title: 'Upload failed', detail: 'Something went wrong. Try again.', retry: true },
};

export function isActive(item: UploadItem): boolean {
  return item.phase === 'QUEUED' || item.phase === 'UPLOADING' || item.phase === 'PROCESSING';
}
