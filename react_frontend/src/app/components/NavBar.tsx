"use client";
import * as React from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import { useRouter } from 'next/navigation';


export default function NavBar(){

  const router = useRouter();

  const navigateToRegistration = () => {
    router.push("/regist");
  }

  const navigateToLogin = () =>{
    router.push("/login");
  }

  const navigateToStartSeite = () => {
    router.push("/");
  }


  return(
        <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Willkommen zu unserem Raumbuchungssystem
          </Typography>
          <Button color="inherit" onClick={navigateToStartSeite}>StartSeite</Button>
          <Button color="inherit" onClick={navigateToRegistration}>Registration</Button>
          <Button color="inherit" onClick={navigateToLogin}>Login</Button>
        </Toolbar>
      </AppBar>
    </Box>
        
    )
}