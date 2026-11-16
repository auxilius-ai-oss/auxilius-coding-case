import CancelIcon from '@mui/icons-material/Close';
import DoneIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import RetryIcon from '@mui/icons-material/Refresh';
import ScheduleIcon from '@mui/icons-material/Schedule';
import UploadIcon from '@mui/icons-material/CloudUpload';
import { Box, Button, IconButton, LinearProgress, Paper, Stack, Tooltip, Typography } from '@mui/material';
import { useConfirm } from '../../shared/ConfirmProvider';
import { formatBytes, formatDuration } from '../../format';
import { CONFIRM_CANCEL_BYTES, ERROR_COPY, isActive, type UploadItem } from './uploadModel';

interface Props {
  items: UploadItem[];
  onCancel: (key: string) => void;
  onCancelAll: () => void;
  onRetry: (key: string) => void;
  onDismiss: (key: string) => void;
}

function speed(item: UploadItem): number | null {
  if (!item.startedAt) return null;
  const seconds = (Date.now() - item.startedAt) / 1000;
  return seconds < 1.5 ? null : item.sent / seconds;
}

function caption(item: UploadItem): string {
  switch (item.phase) {
    case 'QUEUED':
      return 'Waiting to start';
    case 'UPLOADING': {
      const bps = speed(item);
      const progress = `${formatBytes(item.sent)} of ${formatBytes(item.file.size)}`;
      if (!bps) return `${progress} · Measuring speed…`;
      return `${progress} · ${formatBytes(bps)}/s · about ${formatDuration((item.file.size - item.sent) / bps)} left`;
    }
    case 'PROCESSING':
      return `All ${formatBytes(item.file.size)} sent · finishing on the server…`;
    case 'DONE':
      return 'Uploaded';
    case 'FAILED':
      return item.error ? `${ERROR_COPY[item.error].title}. ${ERROR_COPY[item.error].detail}` : 'Failed';
  }
}

function PhaseIcon({ item }: { item: UploadItem }) {
  if (item.phase === 'DONE') return <DoneIcon color="success" fontSize="small" />;
  if (item.phase === 'FAILED') return <ErrorIcon color="error" fontSize="small" />;
  if (item.phase === 'QUEUED') return <ScheduleIcon color="disabled" fontSize="small" />;
  return <UploadIcon color="primary" fontSize="small" />;
}

export function UploadQueue({ items, onCancel, onCancelAll, onRetry, onDismiss }: Props) {
  const confirm = useConfirm();
  if (items.length === 0) return null;

  const active = items.filter(isActive);
  const failed = items.filter((i) => i.phase === 'FAILED').length;
  const total = active.reduce((sum, i) => sum + i.file.size, 0);
  const sent = active.reduce((sum, i) => sum + i.sent, 0);

  const cancel = async (item: UploadItem) => {
    if (item.sent >= CONFIRM_CANCEL_BYTES) {
      const ok = await confirm({
        title: 'Cancel upload?',
        description: `${formatBytes(item.sent)} of "${item.file.name}" has been sent. You will have to start again.`,
        confirmLabel: 'Cancel upload',
        cancelLabel: 'Keep uploading',
        destructive: true,
      });
      if (!ok) return;
    }
    onCancel(item.key);
  };

  const cancelAll = async () => {
    if (sent >= CONFIRM_CANCEL_BYTES) {
      const ok = await confirm({
        title: 'Cancel all uploads?',
        description: `${formatBytes(sent)} of ${formatBytes(total)} has been sent.`,
        confirmLabel: 'Cancel all',
        cancelLabel: 'Keep uploading',
        destructive: true,
      });
      if (!ok) return;
    }
    onCancelAll();
  };

  return (
    <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
      <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between", mb: 1 }}>
        <Box>
          <Typography variant="subtitle2">
            {active.length > 0
              ? `Uploading ${active.length} file${active.length === 1 ? '' : 's'} · ${formatBytes(sent)} of ${formatBytes(total)}`
              : 'Uploads'}
            {failed > 0 && ` · ${failed} failed`}
          </Typography>
          {active.length > 0 && (
            <Typography variant="caption" color="text.secondary">
              Keep this tab open until it finishes.
            </Typography>
          )}
        </Box>
        {active.length > 1 && (
          <Button size="small" onClick={cancelAll}>
            Cancel all
          </Button>
        )}
      </Stack>
      <Stack spacing={1}>
        {items.map((item) => (
          <Stack key={item.key} direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
            <PhaseIcon item={item} />
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="body2" noWrap title={item.file.name}>
                {item.file.name}
              </Typography>
              {(item.phase === 'UPLOADING' || item.phase === 'PROCESSING') && (
                <LinearProgress
                  variant={item.phase === 'PROCESSING' ? 'indeterminate' : 'determinate'}
                  value={item.file.size ? (item.sent / item.file.size) * 100 : 0}
                  sx={{ my: 0.5 }}
                />
              )}
              <Typography variant="caption" color={item.phase === 'FAILED' ? 'error' : 'text.secondary'}>
                {caption(item)}
              </Typography>
            </Box>
            {item.phase === 'FAILED' && item.error && ERROR_COPY[item.error].retry && (
              <Tooltip title="Retry">
                <IconButton size="small" onClick={() => onRetry(item.key)}>
                  <RetryIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {item.phase === 'FAILED' && (
              <Button size="small" onClick={() => onDismiss(item.key)}>
                Dismiss
              </Button>
            )}
            {isActive(item) && (
              <Tooltip title="Cancel">
                <IconButton size="small" onClick={() => cancel(item)}>
                  <CancelIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </Stack>
        ))}
      </Stack>
    </Paper>
  );
}
