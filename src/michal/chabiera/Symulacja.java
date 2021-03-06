package michal.chabiera;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Symulacja {

    public double lambda;
    public double mi;
    public ExponentialDistribution rozkladLambda;
    public ExponentialDistribution rozkladMi;

    public int obsluzone;
    public int zapytania;
    public int przyjsciaOdrzucone;
    public int rozmiarBufora;
    public int liczbaKlientow;
    public int liczbaPomiarowKlientow;
    public int liczbaPomiarowKlientowBufor;
    public int liczbaKlientowBufor;

    public double czasSymulacji;
    public double maxCzasSymulacji;
    public boolean stanSerwera = true;

    public List<Zdarzenie> listaZdarzen;
    public List<Zdarzenie> listaWyjsc;
    public List<Zdarzenie> listaZdarzenHistoria;

    public Map<Integer, Integer> prawdopodobienstwa;

    public Symulacja() throws FileNotFoundException {
        inicjalizuj();
        symuluj();


    }

    private void symuluj() throws FileNotFoundException {
        while (czasSymulacji < maxCzasSymulacji) {
            if (listaZdarzen.size() != 0) {
                Zdarzenie tmp = listaZdarzen.get(0);
                listaZdarzen.remove(0);
                obsluzZdarzenie(tmp);
            }
        }
        statystyki();
        zapiszDoPliku();
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
                    if(iloscWKolejce < rozmiarBufora || rozmiarBufora == 0){
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
                rozkladPrawdopodobienstw();
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
    private void rozkladPrawdopodobienstw(){
        int liczbaWSystemie = (int) listaZdarzen.stream().filter(x -> x.getTyp() == Zdarzenie.typZdarzenia.WYJSCIE).count();
        System.out.println("liczba w systemie: " + liczbaWSystemie);

        prawdopodobienstwa.put(liczbaWSystemie, prawdopodobienstwa.get(liczbaWSystemie) == null ? 1 : prawdopodobienstwa.get(liczbaWSystemie) +1);

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
            try {

                czasPrzyjscia = listaZdarzenHistoria.stream().filter(x -> x.getTyp() == Zdarzenie.typZdarzenia.PRZYJSCIE && x.getId() == wyjscie.getId()).findFirst().get().getCzas();
            } catch (Exception ex) {

                String asd = "asd";
            }

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
        System.out.println("Naplynelo:\t" + zapytania + "\tzadan!");
        System.out.println("Obsluzone zapytania:\t" + obsluzone);
        System.out.println("Odrzucone zapytania:\t" + przyjsciaOdrzucone);
        System.out.println("Srednia liczba klientow:\t" + (double) liczbaKlientow / liczbaPomiarowKlientow);
        System.out.println("Srednia liczba klientow w buforze:\t" + (double) liczbaKlientowBufor / liczbaPomiarowKlientowBufor);
        System.out.println("Sredni czas w systemie:\t" + obliczSredniCzasWSystemie());
        System.out.println("Sredni czas w buforze:\t" + obliczSredniCzasWBuforze());
        System.out.println("Prawdopodobienstwa:");

        for (Integer key : prawdopodobienstwa.keySet()) {
            System.out.println(key + "\t" + (double) prawdopodobienstwa.get(key)/zapytania);
        }
    }
    public void zapiszDoPliku() throws FileNotFoundException {

        PrintWriter zapis = new PrintWriter("D:\\Users\\Michal\\IdeaProjects\\OAST-MM1\\wynik.txt");

        zapis.println("Naplynelo:\t" + zapytania + "\tzadan!");
        zapis.println("Obsluzone zapytania:\t" + obsluzone);
        zapis.println("Odrzucone zapytania:\t" + przyjsciaOdrzucone);
        zapis.println("Srednia liczba klientow:\t" + (double) liczbaKlientow / liczbaPomiarowKlientow);
        zapis.println("Srednia liczba klientow w buforze:\t" + (double) liczbaKlientowBufor / liczbaPomiarowKlientowBufor);
        zapis.println("Sredni czas w systemie:\t" + obliczSredniCzasWSystemie());
        zapis.println("Sredni czas w buforze:\t" + obliczSredniCzasWBuforze());
        zapis.println("Prawdopodobienstwa:");

        for (Integer key : prawdopodobienstwa.keySet()) {
            zapis.println(key + "\t" + (double) prawdopodobienstwa.get(key)/zapytania);
        }
        zapis.close();
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

        rozmiarBufora = 0;
        maxCzasSymulacji = 10000;
        czasSymulacji = 0;
        stanSerwera = true;
        listaZdarzen = new ArrayList<Zdarzenie>();
        listaZdarzenHistoria = new ArrayList<Zdarzenie>();
        listaWyjsc = new ArrayList<Zdarzenie>();

        prawdopodobienstwa = new HashMap<Integer, Integer>();

        rozkladLambda = new ExponentialDistribution(lambda);
        rozkladMi = new ExponentialDistribution(1 / mi);
        dodajZdarzenie(czasSymulacji, Zdarzenie.typZdarzenia.PRZYJSCIE, null);
    }

}
