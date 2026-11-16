import { Alert, Box, CircularProgress, List, ListItemButton, ListItemText, Paper, Typography } from '@mui/material';
import { Link } from 'react-router';
import { useDataSources } from '../datasets/hooks';

export function DataSourceList() {
  const { data, isLoading, error } = useDataSources();
  return (
    <Box sx={{ p: 3, maxWidth: 720 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Data Sources
      </Typography>
      {isLoading && <CircularProgress />}
      {error && <Alert severity="error">Failed to load data sources. Please try again.</Alert>}
      {data && (
        <Paper variant="outlined">
          <List disablePadding>
            {data.map((s) => (
              <ListItemButton key={s.id} component={Link} to={`/data-sources/${s.id}`} divider>
                <ListItemText primary={s.name} secondary="File" />
              </ListItemButton>
            ))}
          </List>
        </Paper>
      )}
    </Box>
  );
}
