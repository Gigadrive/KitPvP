package eu.thechest.kitpvp.kit;

import java.util.ArrayList;

/**
 * Created by zeryt on 20.02.2017.
 */
public class KitStorage {
    public static ArrayList<Kit> STORAGE = new ArrayList<Kit>();

    public static Kit fromID(int id){
        for(Kit k : STORAGE){
            if(k.getID() == id){
                return k;
            }
        }

        return null;
    }

    public static void init(){
        new Assassin();
        new Teleporter();
        new Devil();
        new Booster();
        new Pin();
        new Bomberman();
        new Hulk();
    }
}
