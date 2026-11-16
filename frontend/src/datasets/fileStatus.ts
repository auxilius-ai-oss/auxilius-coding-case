import type { DataFile } from '../api/types';

export type FileStatus =
  | 'UPLOADING'
  | 'UPLOAD_FAILED'
  | 'UPLOADED'
  | 'INFERRING_SCHEMA'
  | 'SCHEMA_FAILED'
  | 'SCHEMA_READY'
  | 'SCHEMA_READY_WITH_WARNINGS';

export const STATUS_LABEL: Record<FileStatus, string> = {
  UPLOADING: 'Uploading',
  UPLOAD_FAILED: 'Upload failed',
  UPLOADED: 'Uploaded',
  INFERRING_SCHEMA: 'Processing',
  SCHEMA_FAILED: 'Error',
  SCHEMA_READY: 'Processed',
  SCHEMA_READY_WITH_WARNINGS: 'Warnings',
};

export const STATUS_COLOR: Record<FileStatus, 'default' | 'error' | 'primary' | 'info' | 'success' | 'warning'> = {
  UPLOADING: 'default',
  UPLOAD_FAILED: 'error',
  UPLOADED: 'primary',
  INFERRING_SCHEMA: 'info',
  SCHEMA_FAILED: 'error',
  SCHEMA_READY: 'success',
  SCHEMA_READY_WITH_WARNINGS: 'warning',
};

const LIFECYCLE: FileStatus[] = [
  'UPLOADING',
  'UPLOAD_FAILED',
  'UPLOADED',
  'INFERRING_SCHEMA',
  'SCHEMA_FAILED',
  'SCHEMA_READY_WITH_WARNINGS',
  'SCHEMA_READY',
];

export function findingCount(file: DataFile): number {
  return file.schemaFindings.reduce((sum, sheet) => sum + sheet.length, 0);
}

/** {@code inferring} marks files whose inference was just requested, before the server says so. */
export function fileStatus(file: DataFile, inferring = false): FileStatus {
  if (file.uploadState === 'UPLOADING') return 'UPLOADING';
  if (file.uploadState === 'UPLOAD_FAILED') return 'UPLOAD_FAILED';
  if (inferring || file.schemaState === 'INFERRING_SCHEMA') return 'INFERRING_SCHEMA';
  switch (file.schemaState) {
    case 'SCHEMA_NOT_INFERRED':
      return 'UPLOADED';
    case 'INFERRING_SCHEMA_FAILED':
      return 'SCHEMA_FAILED';
    default:
      return findingCount(file) > 0 ? 'SCHEMA_READY_WITH_WARNINGS' : 'SCHEMA_READY';
  }
}

export function compareStatus(a: FileStatus, b: FileStatus): number {
  return LIFECYCLE.indexOf(a) - LIFECYCLE.indexOf(b);
}

/** Nothing more will happen to the file without the user doing something. */
export function isSettled(file: DataFile): boolean {
  return file.uploadState !== 'UPLOADING' && file.schemaState !== 'INFERRING_SCHEMA';
}

export function canInfer(file: DataFile): boolean {
  return (
    file.uploadState === 'UPLOADED' &&
    (file.schemaState === 'SCHEMA_NOT_INFERRED' || file.schemaState === 'INFERRING_SCHEMA_FAILED')
  );
}
