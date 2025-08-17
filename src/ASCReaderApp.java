
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ASCReaderApp extends JFrame {

    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JLabel patientInfoLabel;
    private JLabel statusLabel;
    private JButton loadFileButton;
    private JButton clearTableButton;

    private JButton previousPageButton;
    private JButton nextPageButton;
    private JLabel pageInfoLabel;

    private List<String[]> allRecords = new ArrayList<>();
    private int currentPage = 0;
    private final int RECORDS_PER_PAGE = 100;

    private String patientId = "";
    private String patientName = "";
    private String glucoseLimits = "";

    private final String[] columnNames = {
        "Fecha", "Hora", "Glucosa (mg/dl)", "Insulina (U)",
        "Carbohidratos (g)", "Código Evento", "Estado"
    };

    public ASCReaderApp() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Lector de Archivos ASC - Sistema Médico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Información del Paciente"));
        patientInfoLabel = new JLabel("No hay archivo cargado");
        patientInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        topPanel.add(patientInfoLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        loadFileButton = new JButton("Cargar Archivo ASC");
        loadFileButton.setFont(new Font("Arial", Font.BOLD, 14));
        loadFileButton.addActionListener(new LoadFileListener());
        buttonPanel.add(loadFileButton);

        clearTableButton = new JButton("Limpiar Tabla");
        clearTableButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearTableButton.addActionListener(new ClearTableListener());
        buttonPanel.add(clearTableButton);

        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.getTableHeader().setReorderingAllowed(false);

        dataTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Fecha
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // Hora
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Glucosa
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Insulina
        dataTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Carbohidratos
        dataTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Código
        dataTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Estado

        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Registros Médicos"));
        add(scrollPane, BorderLayout.CENTER);

        JPanel paginationPanel = new JPanel(new BorderLayout());

        JPanel paginationControls = new JPanel(new FlowLayout(FlowLayout.CENTER));

        previousPageButton = new JButton("← Anterior");
        previousPageButton.addActionListener(e -> previousPage());
        previousPageButton.setEnabled(false);
        paginationControls.add(previousPageButton);

        pageInfoLabel = new JLabel("Página 0 de 0 (0 registros)");
        pageInfoLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        paginationControls.add(pageInfoLabel);

        nextPageButton = new JButton("Siguiente →");
        nextPageButton.addActionListener(e -> nextPage());
        nextPageButton.setEnabled(false);
        paginationControls.add(nextPageButton);

        paginationPanel.add(paginationControls, BorderLayout.CENTER);

        statusLabel = new JLabel("Listo para cargar archivo");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        paginationPanel.add(statusLabel, BorderLayout.SOUTH);

        add(paginationPanel, BorderLayout.SOUTH);

        setSize(900, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 550));
    }

    private class LoadFileListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos ASC (*.asc)", "asc"));

            int result = fileChooser.showOpenDialog(ASCReaderApp.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                loadASCFile(selectedFile);
            }
        }
    }

    private void loadASCFile(File file) {
        try {
            statusLabel.setText("Cargando archivo: " + file.getName());

            allRecords = parseASCFile(file);
            currentPage = 0;

            updatePatientInfo();

            updateTableWithCurrentPage();

            statusLabel.setText("Archivo cargado exitosamente. " + allRecords.size() + " registros encontrados.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar el archivo: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error al cargar el archivo");
        }
    }

    private List<String[]> parseASCFile(File file) throws IOException {
        List<String[]> records = new ArrayList<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyMMdd");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inDataSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Saltar líneas vacías
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("Patient ID:")) {
                    patientId = line.substring(11).trim();
                    continue;
                }

                if (line.startsWith("Patient Name:")) {
                    patientName = line.substring(13).trim();
                    continue;
                }

                if (line.contains("Glucose Limits")) {
                    glucoseLimits = line;
                    continue;
                }

                if (line.contains("YYMMDD HHMM") || line.contains("Data:")) {
                    inDataSection = true;
                    continue;
                }

                if (inDataSection && line.matches("\\d{6}\\s+\\d{4}.*")) {
                    String[] record = parseDataLine(line, inputDateFormat, outputDateFormat);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        }

        return records;
    }

    private String[] parseDataLine(String line, SimpleDateFormat inputDateFormat, SimpleDateFormat outputDateFormat) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                return null;
            }

            String dateStr = parts[0];
            Date date = inputDateFormat.parse(dateStr);
            String formattedDate = outputDateFormat.format(date);

            String timeStr = parts[1];
            String formattedTime = timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);

            // Parsear glucosa
            String glucose = parts.length > 2 ? parts[2] : "N/A";

            // Parsear insulina
            String insulin = parts.length > 3 ? parts[3] : "N/A";

            // Parsear carbohidratos
            String carbs = parts.length > 4 ? parts[4] : "N/A";

            // Parsear código de evento
            String eventCode = parts.length > 5 ? parts[5] : "N/A";

            // Parsear estado
            String status = parts.length > 6 ? parts[6] : "N/A";

            return new String[]{
                formattedDate, formattedTime, glucose, insulin, carbs, eventCode, status
            };

        } catch (ParseException e) {
            System.err.println("Error parsing date: " + line);
            return null;
        }
    }

    private void updatePatientInfo() {
        StringBuilder info = new StringBuilder();

        if (!patientId.isEmpty()) {
            info.append("ID: ").append(patientId).append(" | ");
        }

        if (!patientName.isEmpty()) {
            info.append("Nombre: ").append(patientName).append(" | ");
        }

        if (!glucoseLimits.isEmpty()) {
            info.append(glucoseLimits);
        }

        if (info.length() == 0) {
            info.append("Información del paciente no disponible");
        }

        patientInfoLabel.setText(info.toString());
    }

    private class ClearTableListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllData();
        }
    }

    private void clearAllData() {
        allRecords.clear();
        currentPage = 0;
        tableModel.setRowCount(0);

        patientId = "";
        patientName = "";
        glucoseLimits = "";

        patientInfoLabel.setText("No hay archivo cargado");
        statusLabel.setText("Tabla limpiada - Listo para cargar archivo");
        updatePaginationControls();
    }

    private void updateTableWithCurrentPage() {
        tableModel.setRowCount(0);

        if (allRecords.isEmpty()) {
            updatePaginationControls();
            return;
        }

        int startIndex = currentPage * RECORDS_PER_PAGE;
        int endIndex = Math.min(startIndex + RECORDS_PER_PAGE, allRecords.size());

        for (int i = startIndex; i < endIndex; i++) {
            tableModel.addRow(allRecords.get(i));
        }

        updatePaginationControls();
    }

    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) allRecords.size() / RECORDS_PER_PAGE);

        if (totalPages == 0) {
            pageInfoLabel.setText("Página 0 de 0 (0 registros)");
            previousPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
        } else {
            int startRecord = currentPage * RECORDS_PER_PAGE + 1;
            int endRecord = Math.min((currentPage + 1) * RECORDS_PER_PAGE, allRecords.size());

            pageInfoLabel.setText(String.format("Página %d de %d (%d-%d de %d registros)",
                    currentPage + 1, totalPages, startRecord, endRecord, allRecords.size()));

            previousPageButton.setEnabled(currentPage > 0);
            nextPageButton.setEnabled(currentPage < totalPages - 1);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateTableWithCurrentPage();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) allRecords.size() / RECORDS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updateTableWithCurrentPage();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ASCReaderApp().setVisible(true);
        });
    }
}
