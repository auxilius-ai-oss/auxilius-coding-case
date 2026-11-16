import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import UploadIcon from '@mui/icons-material/Upload';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useEffect, useRef, useState } from 'react';
import { ApiError, api } from '../api/client';
import type { DataFile, DataSet } from '../api/types';
import { formatBytes } from '../format';
import { useConfirm } from '../shared/ConfirmProvider';
import { useNavigationGuard } from '../shared/useNavigationGuard';
import { FilesGrid } from './FilesGrid';
import { FindingsDialog } from './FindingsDialog';
import { SchemaDialog } from './SchemaDialog';
import { UsedByControls } from './UsedByControls';
import { duplicateNames } from './duplicates';
import { useDataSetMutations, useFileMutations, useFiles } from './hooks';
import { UploadQueue } from './uploads/UploadQueue';
import { isActive } from './uploads/uploadModel';
import { useUploads } from './uploads/useUploads';

const INFER_DEBOUNCE_MS = 2500;
const ACCEPT = '.csv,.xls,.xlsx';

export function FilesPanel({ dataSet }: { dataSet: DataSet }) {
  const { enqueueSnackbar } = useSnackbar();
  const confirm = useConfirm();
  const setMutations = useDataSetMutations(dataSet.dataSourceId);
  const fileMutations = useFileMutations();
  const [inferringIds, setInferringIds] = useState<Set<string>>(new Set());
  const [schemaFile, setSchemaFile] = useState<DataFile | null>(null);
  const [findingsFile, setFindingsFile] = useState<DataFile | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const downloading = useRef(false);

  // Files that finished uploading are inferred together, a moment after the last one lands.
  const batch = useRef<string[]>([]);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const onUploaded = (id: string) => {
    batch.current.push(id);
    setInferringIds((ids) => new Set(ids).add(id));
    clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      const ids = batch.current;
      batch.current = [];
      fileMutations.infer.mutate(ids, {
        onSettled: () =>
          setInferringIds((current) => new Set([...current].filter((id) => !ids.includes(id)))),
      });
    }, INFER_DEBOUNCE_MS);
  };
  useEffect(() => () => clearTimeout(timer.current), []);

  const uploads = useUploads(dataSet.id, onUploaded);
  const activeUploads = uploads.items.filter(isActive);
  const files = useFiles(dataSet.id, activeUploads.length > 0);
  const fileList = files.data ?? [];

  const frozen = dataSet.lockStatus === 'LOCKED' || dataSet.lockStatus === 'LOCKING';
  const lockPending = setMutations.lock.isPending || setMutations.unlock.isPending;
  const anyInferring = inferringIds.size > 0 || fileList.some((f) => f.schemaState === 'INFERRING_SCHEMA');
  const unlockBlocked = dataSet.lockStatus === 'LOCKED' && dataSet.usedByControls.length > 0;

  const sent = activeUploads.reduce((sum, i) => sum + i.sent, 0);
  const total = activeUploads.reduce((sum, i) => sum + i.file.size, 0);
  useNavigationGuard(
    activeUploads.length > 0,
    {
      title: 'Leave and lose your uploads?',
      description: `${formatBytes(sent)} of ${formatBytes(total)} has been sent. Leaving cancels every upload that hasn't finished.`,
      confirmLabel: 'Leave and cancel uploads',
      cancelLabel: 'Stay on page',
      destructive: true,
    },
    uploads.cancelAll,
  );

  // Tell the user how a lock ended.
  const lastStatus = useRef(dataSet.lockStatus);
  useEffect(() => {
    if (lastStatus.current === 'LOCKING' && dataSet.lockStatus === 'LOCKED') {
      enqueueSnackbar('Version locked', { variant: 'success' });
    } else if (lastStatus.current === 'LOCKING' && dataSet.lockStatus === 'LOCK_FAILED') {
      enqueueSnackbar('Locking failed', { variant: 'error' });
    }
    lastStatus.current = dataSet.lockStatus;
  }, [dataSet.lockStatus, enqueueSnackbar]);

  const pickFiles = async (picked: File[]) => {
    const existing = [...fileList.map((f) => f.fileName), ...activeUploads.map((u) => u.file.name)];
    const clashes = duplicateNames(
      picked.map((f) => f.name),
      existing,
    );
    if (clashes.length > 0) {
      await confirm({
        title: 'Upload cancelled',
        description: (
          <>
            These files have the same name as a file in this version:
            <Box component="ul" sx={{ mb: 0 }}>
              {clashes.map((n) => (
                <li key={n}>{n}</li>
              ))}
            </Box>
          </>
        ),
        confirmLabel: 'Got it',
        cancelLabel: null,
      });
      return;
    }
    uploads.add(picked);
  };

  const lock = () =>
    setMutations.lock.mutate(dataSet.id, {
      onError: (e) =>
        enqueueSnackbar(
          (e as ApiError).status === 409 ? 'This version is used by controls' : 'Locking could not start',
          { variant: 'error' },
        ),
    });

  const unlock = async (dismiss: boolean) => {
    if (!dismiss) {
      const ok = await confirm({
        title: 'Unlock version?',
        description: `Controls can't read "${dataSet.name}" until it is locked again.`,
        confirmLabel: 'Unlock',
      });
      if (!ok) return;
    }
    setMutations.unlock.mutate(dataSet.id, {
      onError: (e) =>
        enqueueSnackbar(
          (e as ApiError).status === 409 ? 'This version is used by controls' : 'Unlocking failed',
          { variant: 'error' },
        ),
    });
  };

  const deleteFile = async (file: DataFile) => {
    const ok = await confirm({
      title: 'Delete file?',
      description: `Are you sure you want to delete "${file.fileName}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    });
    if (!ok) return;
    fileMutations.remove.mutate(file.id, {
      onSuccess: () => enqueueSnackbar('File deleted', { variant: 'success' }),
      onError: () => enqueueSnackbar('Deleting the file failed', { variant: 'error' }),
    });
  };

  const infer = (file: DataFile) => {
    setInferringIds((ids) => new Set(ids).add(file.id));
    fileMutations.infer.mutate([file.id], {
      onSettled: () =>
        setInferringIds((ids) => {
          const next = new Set(ids);
          next.delete(file.id);
          return next;
        }),
    });
  };

  const download = (file: DataFile) => {
    if (downloading.current) return;
    downloading.current = true;
    const a = document.createElement('a');
    a.href = api.downloadUrl(file.id);
    a.download = file.fileName;
    a.click();
    setTimeout(() => (downloading.current = false), 1000);
  };

  const lockTooltip = unlockBlocked ? (
    <UsedByControls controls={dataSet.usedByControls} action="unlock" />
  ) : dataSet.lockStatus === 'LOCKED' ? (
    'Unlock'
  ) : (
    'Lock this version so controls can use it'
  );

  return (
    <Box sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', p: 3 }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 2 }}>
        <Typography variant="h6" noWrap title={dataSet.name} sx={{ minWidth: 0 }}>
          {dataSet.name}
        </Typography>
        {dataSet.current && <Chip size="small" color="success" label="Current" />}
        {dataSet.lockStatus === 'LOCKED' && <Chip size="small" icon={<LockIcon />} label="Locked" />}
        {dataSet.lockStatus === 'LOCKING' && (
          <Chip size="small" icon={<CircularProgress size={12} />} label="Locking…" />
        )}
        {dataSet.lockStatus === 'LOCK_FAILED' && (
          <Chip size="small" color="error" variant="outlined" label="Lock failed" />
        )}
        <Box sx={{ flex: 1 }} />
        <Tooltip title={lockTooltip}>
          <span>
            <IconButton
              aria-label={dataSet.lockStatus === 'LOCKED' ? 'Unlock' : 'Lock'}
              disabled={
                activeUploads.length > 0 ||
                anyInferring ||
                dataSet.lockStatus === 'LOCKING' ||
                lockPending ||
                unlockBlocked
              }
              onClick={() => (dataSet.lockStatus === 'LOCKED' ? unlock(false) : lock())}
            >
              {dataSet.lockStatus === 'LOCKED' ? <LockOpenIcon /> : <LockIcon />}
            </IconButton>
          </span>
        </Tooltip>
        <Button
          variant="contained"
          startIcon={<UploadIcon />}
          disabled={files.isLoading || frozen || lockPending}
          onClick={() => inputRef.current?.click()}
        >
          Upload
        </Button>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept={ACCEPT}
          hidden
          onChange={(e) => {
            const picked = Array.from(e.target.files ?? []);
            e.target.value = '';
            if (picked.length) pickFiles(picked);
          }}
        />
      </Stack>

      {dataSet.lockStatus === 'LOCK_FAILED' && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={
            <Stack direction="row" spacing={1}>
              <Button color="inherit" size="small" onClick={lock} disabled={lockPending}>
                Try again
              </Button>
              <Button color="inherit" size="small" onClick={() => unlock(true)} disabled={lockPending}>
                Dismiss
              </Button>
            </Stack>
          }
        >
          <AlertTitle>Locking failed</AlertTitle>
          {dataSet.lockError}
        </Alert>
      )}

      <UploadQueue
        items={uploads.items}
        onCancel={uploads.cancel}
        onCancelAll={uploads.cancelAll}
        onRetry={uploads.retry}
        onDismiss={uploads.dismiss}
      />

      {files.isLoading ? (
        <CircularProgress />
      ) : fileList.length === 0 && uploads.items.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="subtitle1">{frozen ? 'This version is locked' : 'No files yet'}</Typography>
          {!frozen && (
            <Typography variant="body2" color="text.secondary">
              Upload CSV or Excel files to get started
            </Typography>
          )}
        </Box>
      ) : (
        <Box sx={{ flex: 1, minHeight: 300 }}>
          <FilesGrid
            files={fileList}
            loading={files.isFetching && !files.data}
            inferringIds={inferringIds}
            canDelete={!frozen && !lockPending}
            onOpenSchema={setSchemaFile}
            onOpenFindings={setFindingsFile}
            onInfer={infer}
            onDownload={download}
            onDelete={deleteFile}
          />
        </Box>
      )}

      <SchemaDialog file={schemaFile} onClose={() => setSchemaFile(null)} />
      <FindingsDialog file={findingsFile} onClose={() => setFindingsFile(null)} />
    </Box>
  );
}
