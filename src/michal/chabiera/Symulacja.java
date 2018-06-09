package michal.chabiera;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Symulacja {

    public double lambda;
    public double mi;
    public ExponentialDistribution rozkladLambda;
    public ExponentialDistribution rozkladMi;

    public int obsluzone;
    public int zapytania;
    public int liczbaKlientow;
    public int liczbaPomiarowKlientow;
    public int liczbaPomiarowKlientowBufor;
    public int liczbaKlientowBufor;

    public double czasSymulacji;
    public double maxCzasSymulacji = 10000;
    public boolean stanSerwera = true;

    public List<Zdarzenie> listaZdarzen;
    public List<Zdarzenie> listaWyjsc;

    public Symulacja() {
        inicjalizuj();
        symuluj();


    }

    private void symuluj() {
        while (czasSymulacji < maxCzasSymulacji) {
            if (listaZdarzen.size() != 0) {
                Zdarzenie tmp = listaZdarzen.get(0);
                listaZdarzen.remove(0);
                obsluzZdarzenie(tmp);
            }
        }
        statystyki();
    }

    private void obsluzZdarzenie(Zdarzenie zdarzenie) {

        czasSymulacji = zdarzenie.getCzas();
        switch (zdarzenie.getTyp()) {
            case PRZYJSCIE: //zrobione OK
                zapytania++;
                if (czySerwerWolny()) {
                    stanSerwera = false; // serwer zajety
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE);
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.WYJSCIE);
                } else {
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE);
                    dodajZdarzenie(listaWyjsc.get(listaWyjsc.size() - 1).getCzas(), Zdarzenie.typZdarzenia.WYJSCIE);
                }
                break;
            case WYJSCIE:
                if (!listaZdarzen.stream().anyMatch(x -> x.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE)) {
                    stanSerwera = true; //serwer wolny
                }
                obsluzone++;
                obliczSredniaLiczbeKlientow();
                obliczSredniaLiczbeKlientowBufor();
                break;
        }

    }

    private void obliczSredniaLiczbeKlientow() {
        liczbaPomiarowKlientow++;
        for(Zdarzenie z : listaZdarzen){
            if(z.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE){
                liczbaKlientow++;
            }
        }
//        liczbaKlientow--;
    }
    private void obliczSredniaLiczbeKlientowBufor() {
        liczbaPomiarowKlientowBufor++;
        int x = 0;
        for(Zdarzenie z : listaZdarzen){
            if(z.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE){
                liczbaKlientowBufor++;
                x++;
            }
        }
        if(x != 0){
            liczbaKlientowBufor--;
        }

    }



    private void dodajZdarzenie(double czasSymulacji, Zdarzenie.typZdarzenia typZdarzenia) {
        double czasWylosowany = 0;
        if (typZdarzenia == Zdarzenie.typZdarzenia.PRZYJSCIE) {
            czasWylosowany = rozkladLambda.sample();
            listaZdarzen.add(new Zdarzenie(Zdarzenie.typZdarzenia.PRZYJSCIE, czasSymulacji + czasWylosowany));
            Collections.sort(listaZdarzen);
        } else if (typZdarzenia == Zdarzenie.typZdarzenia.WYJSCIE) {
            czasWylosowany = rozkladMi.sample();
            listaZdarzen.add(new Zdarzenie(Zdarzenie.typZdarzenia.WYJSCIE, czasSymulacji + czasWylosowany));
            Collections.sort(listaZdarzen);
            listaWyjsc.add(new Zdarzenie(Zdarzenie.typZdarzenia.WYJSCIE, czasSymulacji + czasWylosowany));
            Collections.sort(listaWyjsc);
        } else if (typZdarzenia == Zdarzenie.typZdarzenia.NULL) {
            listaZdarzen.add(new Zdarzenie(Zdarzenie.typZdarzenia.NULL));
        }
    }

    public void statystyki() {
        System.out.println("Naplynelo: " + zapytania + " zadan!");
        System.out.println("Obsluzone zapytania: " + obsluzone);
        System.out.println("Srednia liczba klientow: " + (double) liczbaKlientow/liczbaPomiarowKlientow);
        System.out.println("Srednia liczba klientow w buforze: " + (double) liczbaKlientowBufor/liczbaPomiarowKlientowBufor);
    }

    public boolean czySerwerWolny() {
        return stanSerwera;
    }
    public void inicjalizuj() {
        //Parametry
        lambda = 1;
        mi = 1.5;
        obsluzone = 0;
        zapytania = 0;
        liczbaKlientow = 0;
        liczbaPomiarowKlientow = 0;
        liczbaKlientowBufor = 0;
        liczbaPomiarowKlientowBufor = 0;

        czasSymulacji = 0;
        stanSerwera = true;
        listaZdarzen = new ArrayList<Zdarzenie>();
        listaWyjsc = new ArrayList<Zdarzenie>();
        rozkladLambda = new ExponentialDistribution(lambda);
        rozkladMi = new ExponentialDistribution(1/mi);
        dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE);
    }

}
