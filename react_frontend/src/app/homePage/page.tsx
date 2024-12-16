"use client";
import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { Button, Paper, MenuItem, Select, FormControl, InputLabel } from "@mui/material";
import { useState } from "react";
import NavBar from "../components/NavBar";
import TextField from "@mui/material/TextField";


export default function HomePage(){
    return (
        <Box>
            <NavBar/>
            <div>Welcome User</div>
        </Box>
        
    )
}