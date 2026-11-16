import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SnackbarProvider } from 'notistack';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Outlet, RouterProvider, createBrowserRouter } from 'react-router';
import { DataSourcePage } from './datasets/DataSourcePage';
import { ConfirmProvider } from './shared/ConfirmProvider';
import { DataSourceList } from './sources/DataSourceList';

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

const router = createBrowserRouter([
  {
    element: (
      <ConfirmProvider>
        <Outlet />
      </ConfirmProvider>
    ),
    children: [
      { path: '/', element: <DataSourceList /> },
      { path: '/data-sources/:id', element: <DataSourcePage /> },
    ],
  },
]);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={createTheme()}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider maxSnack={3} anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}>
          <RouterProvider router={router} />
        </SnackbarProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
);
