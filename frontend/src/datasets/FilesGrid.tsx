import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import DownloadIcon from '@mui/icons-material/Download';
import FindingsIcon from '@mui/icons-material/ReportProblemOutlined';
import InferIcon from '@mui/icons-material/AutoFixHigh';
import SchemaIcon from '@mui/icons-material/TableChartOutlined';
import { Box, IconButton, Tooltip, Typography } from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import type { DataFile } from '../api/types';
import { formatBytes } from '../format';
import { FileStatusChip } from './FileStatusChip';
import { canInfer, compareStatus, fileStatus, findingCount, type FileStatus } from './fileStatus';

interface Props {
  files: DataFile[];
  loading: boolean;
  inferringIds: Set<string>;
  canDelete: boolean;
  onOpenSchema: (file: DataFile) => void;
  onOpenFindings: (file: DataFile) => void;
  onInfer: (file: DataFile) => void;
  onDownload: (file: DataFile) => void;
  onDelete: (file: DataFile) => void;
}

function Footer({ files }: { files: DataFile[] }) {
  const uploaded = files.filter((f) => f.uploadState === 'UPLOADED');
  const total = uploaded.reduce((sum, f) => sum + f.size, 0);
  return (
    <Box sx={{ px: 2, py: 1, borderTop: 1, borderColor: 'divider' }}>
      <Typography variant="body2" color="text.secondary">
        {uploaded.length} file{uploaded.length === 1 ? '' : 's'} · {formatBytes(total)} total
      </Typography>
    </Box>
  );
}

export function FilesGrid(props: Props) {
  const { files, loading, inferringIds, canDelete } = props;

  const columns: GridColDef<DataFile>[] = [
    {
      field: 'fileName',
      headerName: 'File',
      flex: 1,
      minWidth: 200,
      renderCell: ({ value }) => (
        <Typography variant="body2" noWrap title={value} sx={{ lineHeight: 'inherit' }}>
          {value}
        </Typography>
      ),
    },
    {
      field: 'size',
      headerName: 'Size',
      type: 'number',
      width: 100,
      valueFormatter: (value: number) => formatBytes(value),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 150,
      valueGetter: (_value, row) => fileStatus(row, inferringIds.has(row.id)),
      sortComparator: (a: FileStatus, b: FileStatus) => compareStatus(a, b),
      renderCell: ({ row }) => (
        <FileStatusChip
          file={row}
          inferring={inferringIds.has(row.id)}
          onOpenFindings={() => props.onOpenFindings(row)}
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Uploaded',
      type: 'dateTime',
      width: 180,
      valueGetter: (value: string) => new Date(value),
    },
    {
      field: 'actions',
      headerName: '',
      width: 200,
      sortable: false,
      filterable: false,
      disableColumnMenu: true,
      align: 'right',
      renderCell: ({ row }) => {
        const stop = (fn: () => void) => (e: React.MouseEvent) => {
          e.stopPropagation();
          fn();
        };
        const status = fileStatus(row, inferringIds.has(row.id));
        return (
          <Box>
            {findingCount(row) > 0 && (
              <Tooltip title="View findings">
                <IconButton size="small" onClick={stop(() => props.onOpenFindings(row))}>
                  <FindingsIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {row.schemaState === 'INFERRING_SCHEMA_SUCCESS' && (
              <Tooltip title="View schema">
                <IconButton size="small" onClick={stop(() => props.onOpenSchema(row))}>
                  <SchemaIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {canInfer(row) && !inferringIds.has(row.id) && (
              <Tooltip title="Infer schema">
                <IconButton size="small" onClick={stop(() => props.onInfer(row))}>
                  <InferIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip
              title={
                status === 'UPLOADING'
                  ? 'Still uploading'
                  : status === 'UPLOAD_FAILED'
                    ? 'Upload failed — nothing to download'
                    : 'Download'
              }
            >
              <span>
                <IconButton
                  size="small"
                  disabled={row.uploadState !== 'UPLOADED'}
                  onClick={stop(() => props.onDownload(row))}
                >
                  <DownloadIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            {canDelete && (
              <Tooltip title="Delete">
                <IconButton size="small" onClick={stop(() => props.onDelete(row))}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </Box>
        );
      },
    },
  ];

  return (
    <DataGrid
      rows={files}
      columns={columns}
      loading={loading}
      density="compact"
      disableRowSelectionOnClick
      onRowClick={({ row }) => props.onOpenSchema(row)}
      initialState={{ pagination: { paginationModel: { pageSize: 100 } } }}
      pageSizeOptions={[100]}
      slots={{ footer: () => <Footer files={files} /> }}
      sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
    />
  );
}
