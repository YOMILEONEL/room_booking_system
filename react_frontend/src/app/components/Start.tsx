import * as React from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import NavBar from './NavBar';

export default function Start() {
  return (
    <Box>
    <NavBar/>
    <Box component="main" sx={{ p: 3, width: "100%" }}>
    <Toolbar />
    <Typography variant="h4" sx={{ fontWeight: "bold", mb: 2, color: "#1976d2" }}>
      Willkommen im Raumbuchungssystem
    </Typography>
    <Typography variant="body1" sx={{ lineHeight: 1.8 }}>
    Planen und buchen Sie Ihre Räume einfach und effizient. Mit unserem Raumbuchungssystem behalten Sie den Überblick über verfügbare Räume, Buchungsdetails und Ressourcen. Egal, ob Sie Besprechungsräume, Veranstaltungsräume oder Schulungsräume verwalten möchten – unsere Plattform unterstützt Sie dabei, Zeit und Aufwand zu sparen. Dank der intuitiven Benutzeroberfläche können Sie Buchungen schnell und unkompliziert vornehmen. Nutzen Sie die Möglichkeit, Raumverfügbarkeiten in Echtzeit einzusehen und Ihre Buchungen jederzeit anzupassen. Unsere Lösung ist speziell für Unternehmen, Schulen, Eventmanagement und viele weitere Bereiche entwickelt worden, um Ihre Organisation zu optimieren.
    </Typography>
  </Box>
  <Box>
    <div className='startfotos'>
      <div>
        <img src="https://www.esi-asia.com/wp-content/uploads/2020/03/Room-Boking-System.png" alt="" />
      </div>
      <div>
        <img src="https://roommanager.com/wp-content/uploads/2023/03/Room-Booking-System-1024x683.jpg" alt="" />
      </div>
      
    </div>
  </Box>
  </Box>
  );
}
