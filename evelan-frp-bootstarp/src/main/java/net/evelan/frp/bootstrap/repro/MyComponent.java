package net.evelan.frp.bootstrap.repro;

import net.evelan.frp.bootstrap.annotation.EComponent;
import net.evelan.frp.bootstrap.annotation.EImport;

@EComponent
public class MyComponent {
    @EImport
    String myString;

    public String getMyString() {
        return myString + " !!!";
    }
}
