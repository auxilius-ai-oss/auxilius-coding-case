import CloseIcon from '@mui/icons-material/Close';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  Typography,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { COLUMN_TYPES, type ColumnType, type DataFile, type TableSchema } from '../api/types';
import { useFileMutations, usePreview } from './hooks';

const PREVIEW_LINES = 25;

const PREVIEW_ERRORS: Record<number, string> = {
  400: 'Preview is available for CSV and Excel files only.',
  404: 'This file no longer exists.',
  409: 'Infer the schema first.',
};

function PreviewTab({ file }: { file: DataFile }) {
  const { data, isLoading, error } = usePreview(file.id, PREVIEW_LINES, true);
  const [sheet, setSheet] = useState(0);
  if (isLoading) return <CircularProgress size={24} />;
  if (error)
    return (
      <Alert severity="error">
        {PREVIEW_ERRORS[(error as ApiError).status] ?? 'The preview could not be loaded.'}
      </Alert>
    );
  if (!data || data.sheets.length === 0) return <Typography>No rows.</Typography>;

  const current = data.sheets[Math.min(sheet, data.sheets.length - 1)];
  const columns = current.header.map((h, i) => ({ field: `c${i}`, headerName: h || `(column ${i + 1})`, width: 150 }));
  const rows = current.rows.map((r, i) => ({
    id: i,
    ...Object.fromEntries(r.map((v, c) => [`c${c}`, v])),
  }));

  return (
    <Stack spacing={1}>
      <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
        {data.sheets.length > 1 && (
          <Select size="small" value={sheet} onChange={(e) => setSheet(Number(e.target.value))}>
            {data.sheets.map((s, i) => (
              <MenuItem key={s.tableName} value={i}>
                {s.tableName}
              </MenuItem>
            ))}
          </Select>
        )}
        <Typography variant="body2" color="text.secondary">
          Showing first {current.rows.length} rows
        </Typography>
      </Stack>
      <Box sx={{ height: 480 }}>
        <DataGrid rows={rows} columns={columns} density="compact" hideFooter disableRowSelectionOnClick />
      </Box>
    </Stack>
  );
}

function SchemaTab({
  schema,
  editing,
  onChangeType,
}: {
  schema: TableSchema[];
  editing: boolean;
  onChangeType: (table: number, column: number, type: ColumnType) => void;
}) {
  return (
    <Stack spacing={2}>
      {schema.map((table, t) => (
        <Paper key={table.meta.tableName + t} variant="outlined" sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1 }}>
            <Typography sx={{ fontFamily: 'monospace' }}>{table.meta.tableName}</Typography>
            <Chip size="small" label={`${table.columns.length} cols`} />
          </Stack>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Column</TableCell>
                <TableCell>Type</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {table.columns.map((column, c) => (
                <TableRow key={column.columnName + c}>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{column.columnName}</TableCell>
                  <TableCell>
                    {editing ? (
                      <Select
                        size="small"
                        value={column.columnType}
                        onChange={(e) => onChangeType(t, c, e.target.value as ColumnType)}
                      >
                        {COLUMN_TYPES.map((type) => (
                          <MenuItem key={type} value={type}>
                            {type}
                          </MenuItem>
                        ))}
                      </Select>
                    ) : (
                      column.columnType
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      ))}
    </Stack>
  );
}

export function SchemaDialog({ file, onClose }: { file: DataFile | null; onClose: () => void }) {
  const [tab, setTab] = useState(0);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<TableSchema[]>([]);
  const { updateSchema } = useFileMutations();
  const { enqueueSnackbar } = useSnackbar();

  // Every open starts fresh; unsaved edits are dropped.
  useEffect(() => {
    setTab(0);
    setEditing(false);
    setDraft(file?.schemaData ?? []);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [file?.id]);

  const hasSchema = (file?.schemaData.length ?? 0) > 0;

  const changeType = (t: number, c: number, type: ColumnType) =>
    setDraft((tables) =>
      tables.map((table, ti) =>
        ti !== t
          ? table
          : { ...table, columns: table.columns.map((col, ci) => (ci === c ? { ...col, columnType: type } : col)) },
      ),
    );

  const save = () => {
    if (!file) return;
    updateSchema.mutate(
      { id: file.id, schema: draft },
      {
        onSuccess: () => {
          setEditing(false);
          enqueueSnackbar('Schema saved', { variant: 'success' });
        },
        onError: () => enqueueSnackbar('Saving the schema failed', { variant: 'error' }),
      },
    );
  };

  return (
    <Dialog open={file !== null} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle sx={{ pr: 6 }}>
        {file?.fileName}
        <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }} aria-label="Close">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ px: 3 }}>
        <Tab label="Schema" />
        <Tab label="Preview" disabled={!hasSchema} />
      </Tabs>
      <DialogContent dividers sx={{ minHeight: 400 }}>
        {file && tab === 0 && !hasSchema && (
          <Box>
            <Typography variant="subtitle1">No schema inferred yet</Typography>
            <Typography variant="body2" color="text.secondary">
              Use "Infer schema" from the file list to work out the tables and columns.
            </Typography>
          </Box>
        )}
        {file && tab === 0 && hasSchema && <SchemaTab schema={draft} editing={editing} onChangeType={changeType} />}
        {file && tab === 1 && <PreviewTab file={file} />}
      </DialogContent>
      {tab === 0 && hasSchema && (
        <DialogActions>
          {editing ? (
            <>
              <Button
                onClick={() => {
                  setDraft(file?.schemaData ?? []);
                  setEditing(false);
                }}
              >
                Cancel
              </Button>
              <Button variant="contained" onClick={save} disabled={updateSchema.isPending}>
                Save
              </Button>
            </>
          ) : (
            <Button onClick={() => setEditing(true)}>Edit Schema</Button>
          )}
        </DialogActions>
      )}
    </Dialog>
  );
}
