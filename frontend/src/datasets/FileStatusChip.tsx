import { Chip, CircularProgress } from '@mui/material';
import type { DataFile } from '../api/types';
import { STATUS_COLOR, STATUS_LABEL, fileStatus, findingCount } from './fileStatus';

interface Props {
  file: DataFile;
  inferring: boolean;
  onOpenFindings: () => void;
}

export function FileStatusChip({ file, inferring, onOpenFindings }: Props) {
  const status = fileStatus(file, inferring);
  const spinning = status === 'UPLOADING' || status === 'INFERRING_SCHEMA';
  const clickable = findingCount(file) > 0;
  return (
    <Chip
      size="small"
      label={STATUS_LABEL[status]}
      color={STATUS_COLOR[status]}
      variant={status === 'UPLOADED' ? 'outlined' : 'filled'}
      icon={spinning ? <CircularProgress size={12} color="inherit" /> : undefined}
      onClick={
        clickable
          ? (e) => {
              e.stopPropagation();
              onOpenFindings();
            }
          : undefined
      }
    />
  );
}
