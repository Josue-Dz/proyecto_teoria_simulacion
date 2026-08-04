package unah.hn.engine;

import org.junit.jupiter.api.Test;
import unah.hn.dto.HospitalParameters;
import unah.hn.dto.Intervention;
import unah.hn.dto.InterventionType;
import unah.hn.dto.ModelParameters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los motores sin Spring. Fijan la semantica de la cola, que debe ser
 * la misma en el motor determinista y en el de eventos discretos.
 */
class MotoresHospitalariosTest {

    private static final int HORIZONTE = 200;

    private final HospitalEngine determinista = new HospitalEngine();
    private final HospitalDesEngine estocastico = new HospitalDesEngine();
    private final SeirSeiEngine epidemia = new SeirSeiEngine();

    private HospitalParameters hospital() {
        return new HospitalParameters(150.0, 0.04, 4.0, 4.0, 2.0, 0.04, 14.0, 0.12);
    }

    private ModelParameters modelo() {
        return modeloConInmunidad(0.0);
    }

    private ModelParameters modeloConInmunidad(double sigma) {
        return new ModelParameters(1_342_329.0, 1.5, 0.65, 0.40, 0.40,
                5.0, 10.0, 6.0, 70.0 * 365, 14.0, 10.0, sigma);
    }

    /**
     * La inmunidad previa sale de la cadena de transmisión: arranca en R_h, nunca pasa
     * por S_h, y por tanto reduce la epidemia en lugar de contarse como infectada.
     */
    @Test
    void laInmunidadPreviaSacaGenteDeLaPoblacionSusceptible() {
        double nH = 1_342_329.0;
        EpidemicOutput salida = epidemia.integrate(modeloConInmunidad(0.60), List.of(), 10, 1.0);

        double sh0 = salida.states()[0][SeirSeiEngine.SH];
        double rh0 = salida.states()[0][SeirSeiEngine.RH];

        assertEquals(0.60 * nH, rh0, 1.0, "el 60 % debe empezar inmune");
        assertEquals(nH - 10.0 - 0.60 * nH, sh0, 1.0, "el resto, menos la semilla, susceptible");
        assertEquals(nH, sh0 + rh0 + 10.0, 1.0, "los compartimentos deben sumar la población");
    }

    /**
     * Umbral de inmunidad de grupo: por encima de sigma = 1 - 1/R0^2 el brote no
     * despega, porque R_e(0) = R0*sqrt(1 - sigma) cae por debajo de 1.
     */
    @Test
    void porEncimaDelUmbralDeInmunidadDeGrupoNoHayBrote() {
        double r0 = epidemia.integrate(modelo(), List.of(), 5, 1.0).r0();
        double umbral = 1.0 - 1.0 / (r0 * r0);

        EpidemicOutput debajo = epidemia.integrate(modeloConInmunidad(umbral - 0.15), List.of(), 400, 1.0);
        EpidemicOutput encima = epidemia.integrate(modeloConInmunidad(Math.min(0.99, umbral + 0.10)), List.of(), 400, 1.0);

        double picoDebajo = 0, picoEncima = 0;
        for (int t = 0; t <= 400; t++) {
            picoDebajo = Math.max(picoDebajo, debajo.states()[t][SeirSeiEngine.IH]);
            picoEncima = Math.max(picoEncima, encima.states()[t][SeirSeiEngine.IH]);
        }
        double semilla = 10.0;

        assertTrue(picoDebajo > 1000, "por debajo del umbral la epidemia debe crecer");
        // El pico incluye el dia 0, asi que nunca puede bajar de la semilla: lo que se
        // comprueba es que no la SUPERE, o sea que no hubo crecimiento en ningun momento.
        assertTrue(picoEncima <= semilla + 1e-6, "por encima del umbral el brote no debe crecer");
        assertTrue(encima.states()[400][SeirSeiEngine.IH] < 0.01 * semilla,
                "por encima del umbral el brote se apaga solo");
    }

    /** Demanda constante que desborda una capacidad de 150 camas. */
    private double[] demandaConstante(double casosPorDia) {
        double[] incidencia = new double[HORIZONTE + 1];
        for (int t = 0; t <= HORIZONTE; t++) incidencia[t] = casosPorDia;
        return incidencia;
    }

    /**
     * Todo el que llega termina en uno de cuatro destinos: ingresa, muere esperando,
     * abandona sin cama, o sigue en cola. La cuenta tiene que cuadrar.
     */
    @Test
    void ningunPacienteSePierdeEnElMotorDeterminista() {
        HospitalOutput out = determinista.simulate(
                demandaConstante(5000), hospital(), List.of(), HORIZONTE);

        double ingresados = 0;
        for (double a : out.admissions()) ingresados += a;

        double contabilizados = ingresados + out.totalQueueDeaths()
                + out.totalLeftWithoutBed() + out.finalQueue();

        assertEquals(out.totalHospitalizations(), contabilizados,
                out.totalHospitalizations() * 1e-6,
                "la demanda total no cuadra con la suma de destinos");
    }

    /** Bajo saturacion permanente la cola no puede crecer sin limite: se abandona. */
    @Test
    void laColaNoCreceIndefinidamenteEnNingunMotor() {
        double[] incidencia = demandaConstante(5000);

        HospitalOutput det = determinista.simulate(incidencia, hospital(), List.of(), HORIZONTE);
        HospitalOutput des = estocastico.simulate(incidencia, hospital(), List.of(), HORIZONTE, 1L);

        assertTrue(det.totalLeftWithoutBed() > 0, "el determinista no registra abandonos");
        assertTrue(des.totalLeftWithoutBed() > 0, "el DES no registra abandonos");

        // Con espera maxima de 14 dias y 200 llegando por dia (5000 * 0.04), la cola se
        // estabiliza en el orden de W_max dias de demanda, no en decenas de miles.
        double demandaDiaria = 5000 * 0.04;
        double techoRazonable = demandaDiaria * 14 * 1.5;
        assertTrue(det.queue()[HORIZONTE] < techoRazonable,
                "cola determinista desbordada: " + det.queue()[HORIZONTE]);
        assertTrue(des.queue()[HORIZONTE] < techoRazonable,
                "cola DES desbordada: " + des.queue()[HORIZONTE]);
    }

    /** Ningun paciente puede esperar mas que la espera maxima configurada. */
    @Test
    void laEsperaMediaNoSuperaLaEsperaMaxima() {
        double[] incidencia = demandaConstante(5000);

        HospitalOutput det = determinista.simulate(incidencia, hospital(), List.of(), HORIZONTE);
        HospitalOutput des = estocastico.simulate(incidencia, hospital(), List.of(), HORIZONTE, 1L);

        assertTrue(det.avgWaitDays() <= 14.0, "determinista: " + det.avgWaitDays());
        assertTrue(des.avgWaitDays() <= 14.0, "DES: " + des.avgWaitDays());
    }

    /** Los dos motores deben dar resultados del mismo orden de magnitud. */
    @Test
    void losDosMotoresConvergenEnOrdenDeMagnitud() {
        double[] incidencia = demandaConstante(5000);

        HospitalOutput det = determinista.simulate(incidencia, hospital(), List.of(), HORIZONTE);
        HospitalOutput des = estocastico.simulate(incidencia, hospital(), List.of(), HORIZONTE, 1L);

        double razonMuertes = des.totalQueueDeaths() / det.totalQueueDeaths();
        assertTrue(razonMuertes > 0.5 && razonMuertes < 2.0,
                "muertes DES=" + des.totalQueueDeaths() + " vs det=" + det.totalQueueDeaths());

        double razonCola = des.queue()[HORIZONTE] / det.queue()[HORIZONTE];
        assertTrue(razonCola > 0.5 && razonCola < 2.0,
                "cola DES=" + des.queue()[HORIZONTE] + " vs det=" + det.queue()[HORIZONTE]);
    }

    /** Sin saturacion las camas se vacian segun el servicio exponencial, no segun Euler. */
    @Test
    void lasAltasSiguenElServicioExponencial() {
        // Un unico dia de demanda pequenia: 100 llegadas contra 150 camas, sin cola.
        double[] incidencia = new double[HORIZONTE + 1];
        incidencia[1] = 2500;  // 2500 * 0.04 = 100 pacientes
        HospitalParameters hp = new HospitalParameters(150.0, 0.04, 0.0, 4.0, 2.0, 0.04, 14.0, 0.12);

        HospitalOutput out = determinista.simulate(incidencia, hp, List.of(), HORIZONTE);

        // Al dia siguiente debe quedar 100 * e^(-1/4) = 77.9, no 100 * (1 - 1/4) = 75.
        assertEquals(100 * Math.exp(-0.25), out.occupied()[2], 0.5,
                "las altas no siguen 1 - e^(-mu_s)");
    }

    /** Con LOS < 1 dia el Euler explicito daria de alta a mas pacientes de los que hay. */
    @Test
    void conEstanciaMenorAUnDiaLaOcupacionNoSeVuelveNegativa() {
        HospitalParameters hp = new HospitalParameters(150.0, 0.04, 0.0, 0.5, 2.0, 0.04, 14.0, 0.12);

        HospitalOutput out = determinista.simulate(demandaConstante(3000), hp, List.of(), HORIZONTE);

        for (int t = 0; t <= HORIZONTE; t++) {
            assertTrue(out.occupied()[t] >= 0, "ocupacion negativa en el dia " + t);
            assertTrue(out.discharges()[t] >= 0, "altas negativas en el dia " + t);
        }
    }

    /**
     * La fumigacion mata mosquitos adultos pero no destruye los criaderos: la
     * poblacion debe repoblarse hacia su nivel original.
     */
    @Test
    void laPoblacionDeMosquitosSeRecuperaTrasLaFumigacion() {
        int diaFumigacion = 60;
        List<Intervention> intervenciones =
                List.of(new Intervention(InterventionType.FUMIGATION, diaFumigacion, 0.9));

        EpidemicOutput out = epidemia.integrate(modelo(), intervenciones, 365, 1.0);

        double nvBase = 1.5 * 1_342_329.0;
        double vectoresAntes = totalVectores(out, diaFumigacion);
        double vectoresJustoDespues = totalVectores(out, diaFumigacion + 1);
        double vectoresMuchoDespues = totalVectores(out, diaFumigacion + 120);

        assertTrue(vectoresJustoDespues < vectoresAntes * 0.3,
                "la fumigacion no redujo la poblacion de mosquitos");
        assertTrue(vectoresMuchoDespues > nvBase * 0.9,
                "la poblacion de mosquitos no se recupero: " + vectoresMuchoDespues + " de " + nvBase);
    }

    private double totalVectores(EpidemicOutput out, int dia) {
        double[] estado = out.states()[dia];
        return estado[SeirSeiEngine.SV] + estado[SeirSeiEngine.EV] + estado[SeirSeiEngine.IV];
    }
}
