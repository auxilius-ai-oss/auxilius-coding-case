export type LockStatus = 'UNLOCKED' | 'LOCKING' | 'LOCKED' | 'LOCK_FAILED';
export type UploadState = 'UPLOADING' | 'UPLOAD_FAILED' | 'UPLOADED';
export type SchemaState =
  | 'SCHEMA_NOT_INFERRED'
  | 'INFERRING_SCHEMA'
  | 'INFERRING_SCHEMA_FAILED'
  | 'INFERRING_SCHEMA_SUCCESS';
export type ColumnType = 'VARCHAR' | 'BOOLEAN' | 'INTEGER' | 'BIGINT' | 'DECIMAL' | 'DATE' | 'TIMESTAMP';

export const COLUMN_TYPES: ColumnType[] = ['VARCHAR', 'BOOLEAN', 'INTEGER', 'BIGINT', 'DECIMAL', 'DATE', 'TIMESTAMP'];

export interface DataSource {
  id: string;
  name: string;
  currentDataSetId: string | null;
}

export interface ControlRef {
  id: string;
  name: string;
}

export interface DataSet {
  id: string;
  dataSourceId: string;
  name: string;
  lockStatus: LockStatus;
  locked: boolean;
  lockError: string | null;
  current: boolean;
  createdAt: string;
  usedByControls: ControlRef[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ColumnSchema {
  columnName: string;
  columnType: ColumnType;
  nullable: boolean;
}

export interface TableSchema {
  meta: { tableName: string; sheetName: string | null; rowCount: number };
  columns: ColumnSchema[];
}

export type FindingCode =
  | 'BLANK_COLUMN_NAME'
  | 'DUPLICATE_COLUMN_NAME'
  | 'INVALID_COLUMN_NAME_CHARACTERS'
  | 'COLUMN_NAME_STARTS_WITH_DIGIT'
  | 'RESERVED_COLUMN_NAME'
  | 'COLUMN_NAME_TOO_LONG'
  | 'TABLE_NAME_TOO_LONG'
  | 'MERGED_CELLS';

export interface SchemaFinding {
  severity: 'ERROR' | 'WARNING';
  code: FindingCode;
  sheetName: string | null;
  sheetIndex: number | null;
  columnName: string | null;
  columnIndex: number | null;
  cellRange: string | null;
}

export interface DataFile {
  id: string;
  dataSetId: string;
  fileName: string;
  size: number;
  mimeType: string;
  uploadState: UploadState;
  schemaState: SchemaState;
  schemaData: TableSchema[];
  schemaFindings: SchemaFinding[][];
  csvSeparator: string | null;
  createdAt: string;
}

export interface Preview {
  sheets: { tableName: string; header: string[]; rows: string[][] }[];
}
