package me.nexty.alpacas.utils;

import me.nexty.alpacas.Main;

import java.util.logging.Level;

public class Logger {
    private Main plugin;
    private String buffer;

    public Logger(Main plugin){
        this.plugin = plugin;
    }

    public void write(String data) {
        data += " ";
        this.buffer += data;
    }

    public void print(Level level){
        if(this.plugin.DEBUG)
            this.plugin.getLogger().log(level, buffer);

        this.buffer = "";
    }

    public void print(Level level, String data){
        this.buffer += data;
        print(level);
    }
}
