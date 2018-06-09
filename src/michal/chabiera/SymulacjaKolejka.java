package michal.chabiera;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymulacjaKolejka {

    public double lambda;
    public double mi;
    public ExponentialDistribution rozkladLambda;
    public ExponentialDistribution rozkladMi;

    public int obsluzone;
    public int zapytania;
    public int przyjsciaOdrzucone;
    public int liczbaKlientow;
    public int liczbaPomiarowKlientow;
    public int liczbaPomiarowKlientowBufor;
    public int liczbaKlientowBufor;

    public double czasSymulacji;
    public double maxCzasSymulacji;
    public int rozmiarBufora;
    public boolean stanSerwera = true;

    public List<Zdarzenie> listaZdarzen;
    public List<Zdarzenie> listaWyjsc;
    public List<Zdarzenie> listaZdarzenHistoria;

    public SymulacjaKolejka() {
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
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE, zdarzenie);
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.WYJSCIE, zdarzenie);
                } else {
                    long iloscWKolejce = listaZdarzen.stream().filter(x -> x.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE).count()-1;
                    dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE, zdarzenie);
                    if(iloscWKolejce < rozmiarBufora){
                        dodajZdarzenie(listaWyjsc.get(listaWyjsc.size() - 1).getCzas(), Zdarzenie.typZdarzenia.WYJSCIE, zdarzenie);
                    } else {
                        przyjsciaOdrzucone++;
                    }
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
        for (Zdarzenie z : listaZdarzen) {
            if (z.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE) {
                liczbaKlientow++;
            }
        }
    }

    private void obliczSredniaLiczbeKlientowBufor() {
        liczbaPomiarowKlientowBufor++;
        int x = 0;
        for (Zdarzenie z : listaZdarzen) {
            if (z.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE) {
                liczbaKlientowBufor++;
                x++;
            }
        }
        if (x != 0) {
            liczbaKlientowBufor--;
        }
    }

    private double obliczSredniCzasWSystemie() {
        double czasWyjscia = 0;
        double czasPrzyjscia = 0;
        double czasObslugi = 0;
        for (Zdarzenie wyjscie : listaWyjsc) {
            czasWyjscia = wyjscie.getCzas();
            czasPrzyjscia = listaZdarzenHistoria.stream().filter(x -> x.getTyp() == Zdarzenie.typZdarzenia.PRZYJSCIE && x.getId() == wyjscie.getId()).findFirst().get().getCzas();

            czasObslugi += czasWyjscia - czasPrzyjscia;
        }

        return czasObslugi / listaWyjsc.size();
    }

    private double obliczSredniCzasWBuforze() {
        double czasWyjscia = 0;
        double czasPrzyjscia = 0;
        double czasObslugi = 0;
        for (Zdarzenie wyjscie : listaWyjsc) {
            czasWyjscia = wyjscie.getCzas();
            czasPrzyjscia = listaZdarzenHistoria.stream().filter(x -> x.getTyp() == Zdarzenie.typZdarzenia.PRZYJSCIE && x.getId() == wyjscie.getId()+1).findFirst().get().getCzas();

            if(czasWyjscia - czasPrzyjscia > 0){
                czasObslugi += czasWyjscia - czasPrzyjscia;
            }
        }
        return czasObslugi / listaWyjsc.size();
    }

    private void dodajZdarzenie(double czasSymulacji, Zdarzenie.typZdarzenia typZdarzenia, Zdarzenie zdarzenie) {
        double czasWylosowany = 0;
        if (typZdarzenia == Zdarzenie.typZdarzenia.PRZYJSCIE) {
            czasWylosowany = rozkladLambda.sample();
            Zdarzenie noweZdarzenie = new Zdarzenie(0, Zdarzenie.typZdarzenia.PRZYJSCIE, czasSymulacji + czasWylosowany);
            listaZdarzen.add(noweZdarzenie);
            Collections.sort(listaZdarzen);

            listaZdarzenHistoria.add(noweZdarzenie);
            Collections.sort(listaZdarzenHistoria);
        } else if (typZdarzenia == Zdarzenie.typZdarzenia.WYJSCIE) {
            czasWylosowany = rozkladMi.sample();
            Zdarzenie noweZdarzenie = new Zdarzenie(zdarzenie.getId(), Zdarzenie.typZdarzenia.WYJSCIE, czasSymulacji + czasWylosowany);
            listaZdarzen.add(noweZdarzenie);
            Collections.sort(listaZdarzen);
            listaWyjsc.add(noweZdarzenie);
            Collections.sort(listaWyjsc);

            listaZdarzenHistoria.add(noweZdarzenie);
            Collections.sort(listaZdarzenHistoria);
        } else if (typZdarzenia == Zdarzenie.typZdarzenia.NULL) {
            listaZdarzen.add(new Zdarzenie(Zdarzenie.typZdarzenia.NULL));
        }
    }

    public void statystyki() {
        System.out.println("Naplynelo: " + zapytania + " zadan!");
        System.out.println("Obsluzone zapytania: " + obsluzone);
        System.out.println("Odrzucone zapytania: " + przyjsciaOdrzucone);
        System.out.println("Srednia liczba klientow: " + (double) liczbaKlientow / liczbaPomiarowKlientow);
        System.out.println("Srednia liczba klientow w buforze: " + (double) liczbaKlientowBufor / liczbaPomiarowKlientowBufor);
        System.out.println("Sredni czas w systemie: " + obliczSredniCzasWSystemie());
        System.out.println("Sredni czas w buforze: " + obliczSredniCzasWBuforze());
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
        przyjsciaOdrzucone = 0;
        liczbaKlientow = 0;
        liczbaPomiarowKlientow = 0;
        liczbaKlientowBufor = 0;
        liczbaPomiarowKlientowBufor = 0;

        maxCzasSymulacji = 10000;
        rozmiarBufora = 5;
        czasSymulacji = 0;
        stanSerwera = true;
        listaZdarzen = new ArrayList<Zdarzenie>();
        listaZdarzenHistoria = new ArrayList<Zdarzenie>();
        listaWyjsc = new ArrayList<Zdarzenie>();
        rozkladLambda = new ExponentialDistribution(lambda);
        rozkladMi = new ExponentialDistribution(1 / mi);
        dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE, null);
    }

}
