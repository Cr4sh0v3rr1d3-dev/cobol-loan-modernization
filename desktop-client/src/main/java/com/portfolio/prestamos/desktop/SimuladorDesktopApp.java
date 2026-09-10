package com.portfolio.prestamos.desktop;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimuladorDesktopApp extends Application {

    private static final String ENDPOINT = "http://localhost:8080/api/v1/prestamos/simular";

    private static final NumberFormat FORMATO_ARS = NumberFormat.getNumberInstance(new Locale("es", "AR"));
    static {
        FORMATO_ARS.setMinimumFractionDigits(2);
        FORMATO_ARS.setMaximumFractionDigits(2);
    }

    private static final List<BancoProducto> BANCOS = List.of(
            new BancoProducto("Banco Galicia", "Éminent"),
            new BancoProducto("Banco Macro", "Jubilados y Selecta"),
            new BancoProducto("Banco Nación", "Jubilados con descuento"),
            new BancoProducto("Banco Galicia", "Personal Plus"),
            new BancoProducto("Banco Macro", "Plan Sueldo"),
            new BancoProducto("Banco Nación", "Empleados públicos con haberes"),
            new BancoProducto("Banco Ciudad", "Plan Sueldo"),
            new BancoProducto("Banco Santander", "Personal"),
            new BancoProducto("Banco Patagonia", "Personal Online"),
            new BancoProducto("Banco Hipotecario", "Destino Libre"),
            new BancoProducto("BBVA", "Oferta exclusiva para clientes")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ComboBox<BancoProducto> comboBanco;
    private TextField campoMonto;
    private Spinner<Integer> spinnerPlazo;
    private Button botonSimular;
    private Label labelCuota;
    private Label labelTasa;
    private Label labelIntereses;
    private Label labelError;
    private StackedBarChart<String, Number> grafico;

    @Override
    public void start(Stage stage) {
        comboBanco = new ComboBox<>(FXCollections.observableArrayList(BANCOS));
        comboBanco.getSelectionModel().selectFirst();

        campoMonto = new TextField();
        campoMonto.setPromptText("ej. 100000.00 o 100.000,00");

        spinnerPlazo = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 360, 12));
        spinnerPlazo.setEditable(true);

        botonSimular = new Button("Simular");
        botonSimular.setOnAction(e -> simular());

        labelCuota = new Label("—");
        labelTasa = new Label("—");
        labelIntereses = new Label("—");
        labelError = new Label();
        labelError.setStyle("-fx-text-fill: #b00020;");
        labelError.setVisible(false);
        labelError.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.addRow(0, new Label("Banco / producto:"), comboBanco);
        grid.addRow(1, new Label("Monto solicitado:"), campoMonto);
        grid.addRow(2, new Label("Plazo (meses):"), spinnerPlazo);
        grid.addRow(3, botonSimular);
        grid.addRow(4, new Label("Cuota mensual:"), labelCuota);
        grid.addRow(5, new Label("Tasa anual aplicada:"), labelTasa);
        grid.addRow(6, new Label("Total intereses:"), labelIntereses);
        grid.addRow(7, labelError);

        CategoryAxis ejeCuotas = new CategoryAxis();
        ejeCuotas.setLabel("Cuota");
        NumberAxis ejeMonto = new NumberAxis();
        ejeMonto.setLabel("Monto ($)");
        grafico = new StackedBarChart<>(ejeCuotas, ejeMonto);
        grafico.setTitle("Composición de cada cuota — sistema francés");
        grafico.setAnimated(false);
        grafico.setVisible(false);
        grafico.setPrefHeight(280);

        VBox raiz = new VBox(15, grid, grafico);
        raiz.setPadding(new Insets(10));

        stage.setScene(new Scene(raiz, 620, 660));
        stage.setTitle("Simulador de préstamos");
        stage.show();
    }

    private void simular() {
        BancoProducto seleccion = comboBanco.getValue();
        BigDecimal monto;
        try {
            monto = parsearMonto(campoMonto.getText());
        } catch (NumberFormatException ex) {
            mostrarError("Monto inválido");
            return;
        }
        int plazo = spinnerPlazo.getValue();

        labelError.setVisible(false);
        botonSimular.setDisable(true);

        Task<ResultadoSimulacion> tarea = new Task<>() {
            @Override
            protected ResultadoSimulacion call() throws Exception {
                return invocarSimulacion(seleccion, monto, plazo);
            }
        };
        tarea.setOnSucceeded(e -> {
            botonSimular.setDisable(false);
            mostrarResultado(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            botonSimular.setDisable(false);
            mostrarError(mensajeDeError(tarea.getException()));
        });
        new Thread(tarea, "simular-prestamo").start();
    }

    private ResultadoSimulacion invocarSimulacion(BancoProducto seleccion, BigDecimal monto, int plazo) throws Exception {
        String cuerpo = "{\"banco\":\"" + escapar(seleccion.banco()) + "\",\"producto\":\"" + escapar(seleccion.producto())
                + "\",\"montoSolicitado\":" + monto.toPlainString() + ",\"plazoMeses\":" + plazo + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String mensaje = extraerCampo(response.body(), "error");
            throw new SimulacionRechazadaException(mensaje != null ? mensaje : "El servicio rechazó la solicitud");
        }

        List<CuotaGrafico> tabla = new ArrayList<>();
        for (String objeto : extraerArrayDeObjetos(response.body(), "tablaAmortizacion")) {
            tabla.add(new CuotaGrafico(
                    Integer.parseInt(extraerCampo(objeto, "numero")),
                    new BigDecimal(extraerCampo(objeto, "interes")),
                    new BigDecimal(extraerCampo(objeto, "amortizacion"))));
        }

        return new ResultadoSimulacion(
                new BigDecimal(extraerCampo(response.body(), "cuotaMensual")),
                new BigDecimal(extraerCampo(response.body(), "tasaAnualAplicada")),
                new BigDecimal(extraerCampo(response.body(), "totalIntereses")),
                tabla
        );
    }

    private void mostrarResultado(ResultadoSimulacion resultado) {
        labelCuota.setText(formatearMoneda(resultado.cuotaMensual()));
        labelTasa.setText(resultado.tasaAnualAplicada().toPlainString() + "%");
        labelIntereses.setText(formatearMoneda(resultado.totalIntereses()));
        actualizarGrafico(resultado.tabla());
    }

    private void actualizarGrafico(List<CuotaGrafico> tabla) {
        grafico.getData().clear();
        if (tabla.isEmpty()) {
            grafico.setVisible(false);
            return;
        }
        XYChart.Series<String, Number> serieInteres = new XYChart.Series<>();
        serieInteres.setName("Interés (renta)");
        XYChart.Series<String, Number> serieAmortizacion = new XYChart.Series<>();
        serieAmortizacion.setName("Amortización");

        for (CuotaGrafico cuota : tabla) {
            String etiqueta = String.valueOf(cuota.numero());
            serieInteres.getData().add(new XYChart.Data<>(etiqueta, cuota.interes()));
            serieAmortizacion.getData().add(new XYChart.Data<>(etiqueta, cuota.amortizacion()));
        }

        grafico.getData().addAll(serieInteres, serieAmortizacion);
        grafico.setVisible(true);
    }

    private void mostrarError(String mensaje) {
        labelError.setText(mensaje);
        labelError.setVisible(true);
        grafico.setVisible(false);
    }

    private static String mensajeDeError(Throwable ex) {
        if (ex instanceof java.net.ConnectException || ex instanceof java.io.IOException) {
            return "No se pudo conectar al servicio — verificá que docker compose esté corriendo.";
        }
        if (ex instanceof SimulacionRechazadaException) {
            return ex.getMessage();
        }
        return "Error inesperado: " + ex.getMessage();
    }

    private static String formatearMoneda(BigDecimal valor) {
        return FORMATO_ARS.format(valor);
    }

    private static BigDecimal parsearMonto(String texto) {
        String limpio = texto.trim();
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(limpio);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extraerCampo(String json, String campo) {
        Pattern patron = Pattern.compile("\"" + campo + "\"\\s*:\\s*(?:\"([^\"]*)\"|(-?[0-9.]+))");
        Matcher m = patron.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    private static List<String> extraerArrayDeObjetos(String json, String campo) {
        Pattern patronArray = Pattern.compile("\"" + campo + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher m = patronArray.matcher(json);
        if (!m.find()) {
            return List.of();
        }
        List<String> objetos = new ArrayList<>();
        Matcher itemMatcher = Pattern.compile("\\{[^{}]*}").matcher(m.group(1));
        while (itemMatcher.find()) {
            objetos.add(itemMatcher.group());
        }
        return objetos;
    }

    private record BancoProducto(String banco, String producto) {
        @Override
        public String toString() {
            return banco + " — " + producto;
        }
    }

    private record CuotaGrafico(int numero, BigDecimal interes, BigDecimal amortizacion) {
    }

    private record ResultadoSimulacion(
            BigDecimal cuotaMensual, BigDecimal tasaAnualAplicada, BigDecimal totalIntereses, List<CuotaGrafico> tabla) {
    }

    private static final class SimulacionRechazadaException extends Exception {
        SimulacionRechazadaException(String mensaje) {
            super(mensaje);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
