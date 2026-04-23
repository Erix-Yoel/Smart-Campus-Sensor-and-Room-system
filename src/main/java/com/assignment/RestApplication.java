package com.assignment;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api/v1")
public class RestApplication extends Application {
    // We let Jersey scan packages in Main.java ResourceConfig, so we don't necessarily 
    // need to override getClasses() here, although we could.
}
