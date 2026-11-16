import AddIcon from '@mui/icons-material/Add';
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  CircularProgress,
  IconButton,
  Link,
  Menu,
  MenuItem,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useState } from 'react';
import { Link as RouterLink, useParams, useSearchParams } from 'react-router';
import { ApiError } from '../api/client';
import type { DataSet } from '../api/types';
import { useConfirm } from '../shared/ConfirmProvider';
import { FilesPanel } from './FilesPanel';
import { UsedByControls } from './UsedByControls';
import { VersionListItem } from './VersionListItem';
import { VersionNameDialog } from './VersionNameDialog';
import { useDataSetMutations, useDataSets, useDataSource } from './hooks';

export function DataSourcePage() {
  const { id = '' } = useParams();
  const [params, setParams] = useSearchParams();
  const selectedId = params.get('datasetId');
  const source = useDataSource(id);
  const dataSets = useDataSets(id);
  const mutations = useDataSetMutations(id);
  const confirm = useConfirm();
  const { enqueueSnackbar } = useSnackbar();
  const [menu, setMenu] = useState<{ anchor: HTMLElement; dataSet: DataSet } | null>(null);
  const [dialog, setDialog] = useState<{ mode: 'create' | 'rename'; dataSet?: DataSet } | null>(null);

  const versions = dataSets.data ?? [];
  const selected = versions.find((d) => d.id === selectedId) ?? null;
  const select = (dataSetId: string) => setParams({ datasetId: dataSetId }, { replace: true });

  const submitName = (name: string) => {
    if (dialog?.mode === 'rename' && dialog.dataSet) {
      mutations.rename.mutate(
        { id: dialog.dataSet.id, name },
        {
          onSuccess: () => {
            setDialog(null);
            enqueueSnackbar('Version renamed', { variant: 'success' });
          },
          onError: () => enqueueSnackbar('Renaming failed', { variant: 'error' }),
        },
      );
    } else {
      mutations.create.mutate(name, {
        onSuccess: (created) => {
          setDialog(null);
          enqueueSnackbar('Version created', { variant: 'success' });
          select(created.id);
        },
        onError: () => enqueueSnackbar('Creating the version failed', { variant: 'error' }),
      });
    }
  };

  const deleteVersion = async (dataSet: DataSet) => {
    const ok = await confirm({
      title: 'Delete version?',
      description: `Are you sure you want to delete "${dataSet.name}"? This also deletes every file uploaded to this version. This action cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    });
    if (!ok) return;
    mutations.remove.mutate(dataSet.id, {
      onSuccess: () => {
        enqueueSnackbar('Version deleted', { variant: 'success' });
        if (dataSet.id === selectedId) setParams({}, { replace: true });
      },
      onError: (e) =>
        enqueueSnackbar(
          (e as ApiError).status === 409 ? 'This version is used by controls' : 'Deleting the version failed',
          { variant: 'error' },
        ),
    });
  };

  if (source.error || dataSets.error) {
    return (
      <Alert severity="error" sx={{ m: 3 }}>
        Failed to load the data source. Please try again.
      </Alert>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <Box sx={{ px: 3, pt: 2, pb: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Breadcrumbs>
          <Link component={RouterLink} to="/" underline="hover" color="inherit">
            Data Sources
          </Link>
          <Typography color="text.primary">{source.data?.name ?? '…'}</Typography>
        </Breadcrumbs>
        <Typography variant="h5" sx={{ mt: 1 }}>
          {source.data?.name}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Configure connection and data model settings
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', flex: 1, minHeight: 0 }}>
        <Box
          sx={{ width: 240, flexShrink: 0, borderRight: 1, borderColor: 'divider', overflowY: 'auto', p: 1 }}
        >
          <Stack direction="row" sx={{ alignItems: "center", px: 1, py: 1 }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" noWrap title={source.data?.name}>
                {source.data?.name}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {versions.length} version{versions.length === 1 ? '' : 's'}
              </Typography>
            </Box>
            <Tooltip title="New version">
              <IconButton size="small" onClick={() => setDialog({ mode: 'create' })}>
                <AddIcon />
              </IconButton>
            </Tooltip>
          </Stack>
          {dataSets.isLoading ? (
            <CircularProgress size={20} sx={{ m: 2 }} />
          ) : versions.length === 0 ? (
            <Box sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                No versions yet
              </Typography>
              <Button size="small" variant="outlined" onClick={() => setDialog({ mode: 'create' })}>
                Create first version
              </Button>
            </Box>
          ) : (
            versions.map((d) => (
              <VersionListItem
                key={d.id}
                dataSet={d}
                selected={d.id === selectedId}
                onSelect={() => select(d.id)}
                onOpenMenu={(anchor) => setMenu({ anchor, dataSet: d })}
              />
            ))
          )}
        </Box>

        {selected ? (
          <FilesPanel key={selected.id} dataSet={selected} />
        ) : (
          <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column' }}>
            <Typography color="text.secondary">Select a version to view its files</Typography>
            {versions.length === 0 && !dataSets.isLoading && (
              <Button sx={{ mt: 2 }} variant="contained" onClick={() => setDialog({ mode: 'create' })}>
                Create first version
              </Button>
            )}
          </Box>
        )}
      </Box>

      <Menu anchorEl={menu?.anchor} open={menu !== null} onClose={() => setMenu(null)}>
        <MenuItem
          onClick={() => {
            setDialog({ mode: 'rename', dataSet: menu!.dataSet });
            setMenu(null);
          }}
        >
          Rename
        </MenuItem>
        <Tooltip
          placement="right"
          title={
            menu && menu.dataSet.usedByControls.length > 0 ? (
              <UsedByControls controls={menu.dataSet.usedByControls} action="delete" />
            ) : (
              ''
            )
          }
        >
          <span>
            <MenuItem
              disabled={!menu || menu.dataSet.usedByControls.length > 0}
              onClick={() => {
                const target = menu!.dataSet;
                setMenu(null);
                deleteVersion(target);
              }}
            >
              Delete
            </MenuItem>
          </span>
        </Tooltip>
      </Menu>

      <VersionNameDialog
        open={dialog !== null}
        mode={dialog?.mode ?? 'create'}
        initialName={dialog?.dataSet?.name ?? ''}
        saving={mutations.create.isPending || mutations.rename.isPending}
        onClose={() => setDialog(null)}
        onSubmit={submitName}
      />
    </Box>
  );
}
