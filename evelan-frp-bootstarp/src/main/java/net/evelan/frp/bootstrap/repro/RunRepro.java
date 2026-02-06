package net.evelan.frp.bootstrap.repro;

import net.evelan.frp.bootstrap.core.AssembleApplicationContext;

public class RunRepro {
    public static void main(String[] args) {
        System.out.println("Starting Context...");
        try {
            new AssembleApplicationContext();
            System.out.println("Context Started Successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
