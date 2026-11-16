import LockIcon from '@mui/icons-material/Lock';
import MoreIcon from '@mui/icons-material/MoreVert';
import StarIcon from '@mui/icons-material/Star';
import { Box, CircularProgress, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import type { DataSet } from '../api/types';
import { formatDate } from '../format';

interface Props {
  dataSet: DataSet;
  selected: boolean;
  onSelect: () => void;
  onOpenMenu: (anchor: HTMLElement) => void;
}

function caption(d: DataSet) {
  switch (d.lockStatus) {
    case 'LOCKING':
      return 'Locking…';
    case 'LOCK_FAILED':
      return 'Lock failed';
    case 'LOCKED':
      return null;
    default:
      return 'Draft';
  }
}

export function VersionListItem({ dataSet, selected, onSelect, onOpenMenu }: Props) {
  const state = caption(dataSet);
  const dot = selected ? 'primary.main' : dataSet.current ? 'success.main' : 'grey.400';
  return (
    <Box
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault();
          onSelect();
        }
      }}
      sx={{
        px: 1.5,
        py: 1,
        cursor: 'pointer',
        borderRadius: 1,
        bgcolor: selected ? 'action.selected' : undefined,
        '&:hover': { bgcolor: selected ? 'action.selected' : 'action.hover' },
        '&:focus-visible': { outline: 2, outlineColor: 'primary.main' },
        '& .menu': { opacity: 0 },
        '&:hover .menu, &:focus-within .menu': { opacity: 1 },
      }}
    >
      <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
        <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: dot, flexShrink: 0 }} />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="body2" noWrap title={dataSet.name} sx={{ fontWeight: selected ? 600 : 400 }}>
            {dataSet.name}
          </Typography>
          <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
            {dataSet.lockStatus === 'LOCKED' && <LockIcon sx={{ fontSize: 12 }} color="action" />}
            <Tooltip title={dataSet.lockStatus === 'LOCK_FAILED' ? (dataSet.lockError ?? '') : ''}>
              <Typography
                variant="caption"
                color={dataSet.lockStatus === 'LOCK_FAILED' ? 'error' : 'text.secondary'}
                noWrap
              >
                {state ? `${state} · ` : ''}
                {formatDate(dataSet.createdAt)}
              </Typography>
            </Tooltip>
          </Stack>
        </Box>
        {dataSet.lockStatus === 'LOCKING' && <CircularProgress size={14} />}
        {dataSet.current && (
          <Tooltip title="Current version">
            <StarIcon sx={{ fontSize: 16 }} color="warning" />
          </Tooltip>
        )}
        <IconButton
          className="menu"
          size="small"
          aria-label={`Actions for ${dataSet.name}`}
          onClick={(e) => {
            e.stopPropagation();
            onOpenMenu(e.currentTarget);
          }}
        >
          <MoreIcon fontSize="small" />
        </IconButton>
      </Stack>
    </Box>
  );
}
