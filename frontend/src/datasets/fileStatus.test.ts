import { describe, expect, it } from 'vitest';
import type { DataFile } from '../api/types';
import { compareStatus, fileStatus } from './fileStatus';

const file = (overrides: Partial<DataFile>): DataFile => ({
  id: '1',
  dataSetId: 'd',
  fileName: 'a.csv',
  size: 1,
  mimeType: 'text/csv',
  uploadState: 'UPLOADED',
  schemaState: 'INFERRING_SCHEMA_SUCCESS',
  schemaData: [],
  schemaFindings: [],
  csvSeparator: 'COMMA',
  createdAt: '2026-01-01T00:00:00Z',
  ...overrides,
});

describe('fileStatus', () => {
  it('reports warnings when a processed file has findings', () => {
    const finding = {
      severity: 'WARNING' as const,
      code: 'MERGED_CELLS' as const,
      sheetName: null,
      sheetIndex: null,
      columnName: null,
      columnIndex: null,
      cellRange: 'A1:B2',
    };
    expect(fileStatus(file({ schemaFindings: [[finding]] }))).toBe('SCHEMA_READY_WITH_WARNINGS');
  });

  it('shows a file as processing as soon as inference is requested', () => {
    expect(fileStatus(file({ schemaState: 'SCHEMA_NOT_INFERRED' }), true)).toBe('INFERRING_SCHEMA');
  });

  it('a failed upload wins over the schema state', () => {
    expect(fileStatus(file({ uploadState: 'UPLOAD_FAILED' }))).toBe('UPLOAD_FAILED');
  });

  it('sorts statuses in lifecycle order', () => {
    expect(compareStatus('UPLOADING', 'SCHEMA_READY')).toBeLessThan(0);
  });
});
