import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class ProgramGUI {
    private static final String     slash;
    private static final int        WIDTH;
    private static final int        HEIGHT;
    static final String             title;
    private static final String     fileExt;
    private static final String     listExt;
    private static final String     propFilename;
    private static JFrame           mainframe;
    private static JFileChooser     urlsChooser;
    private static JFileChooser     fileChooser;
    private static JButton          saveBtn;
    private static JButton          openBtn;
    private static JToggleButton    start;
    private static boolean          startPressed;
    private static JSlider          slider;
    private static String[]         outputFiles;
    static JTextArea                textArea;
    static JLabel                   curUrl;
    static JProgressBar             progressBar;

    static {
        title = "wParser";
        WIDTH = 600;
        HEIGHT = 400;
        slash = File.separator;
        fileExt = ".json";
        listExt = ".lst";
        propFilename = System.getProperty("user.home") + slash + ".wparser";
        startPressed = false;
    }

    private static void initJFrame() {
        JPanel              panel;
        PrintStream         ps;
        TxtOutputStream     tos;
        Font                font;
        URL                 iconURL;
        Image               icon;

        mainframe = new JFrame();
        mainframe.setTitle(title);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            showErrMsg(e, " Error caused by UIManager.getSystemLookAndFeel()");
        }
        font = new Font("Sans-serif", Font.PLAIN, 18);
        progressBar = new JProgressBar();
        fileChooser = new JFileChooser();
        urlsChooser = new JFileChooser();
        openBtn = new JButton();
        saveBtn = new JButton();
        start = new JToggleButton("Start");
        curUrl = new JLabel("");

        mainframe.setSize(WIDTH, HEIGHT);
        mainframe.setResizable(false);
        mainframe.setLocationRelativeTo(null);
        mainframe.setLayout(new BorderLayout());
        if ((iconURL = ProgramGUI.class.getClassLoader().getResource("favicon.ico")) != null) {
            icon = new ImageIcon(iconURL).getImage();
            icon.flush();
            mainframe.setIconImage(icon);
        }

        textArea = new JTextArea(6, 32);
        textArea.setMargin(new Insets(5,10,5,5));
        tos = new TxtOutputStream(textArea);
        ps = new PrintStream(tos);
        System.setOut(ps);
        textArea.setFont(new Font("Sans-serif", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        fileChooser.setDialogTitle("Save to...");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter
                (new FileNameExtensionFilter(fileExt + " only", "json"));

        urlsChooser.setDialogTitle("Choose URL's list");
        urlsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        urlsChooser.setFileFilter
                (new FileNameExtensionFilter(listExt + " only", "lst"));
        progressBar.setStringPainted(true);
        progressBar.setFont(font);
        progressBar.setBorderPainted(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
        progressBar.setPreferredSize(new Dimension(WIDTH - 60, 40));
        progressBar.setForeground(Color.BLACK);

        slider = new JSlider(JSlider.VERTICAL, 1, 8, 5);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setLabelTable(slider.createStandardLabels(1));
        slider.setPreferredSize(new Dimension(60, 150));
        slider.setSnapToTicks(true);
        slider.setVisible(true);

        curUrl.setVerticalAlignment(JLabel.BOTTOM);
        curUrl.setFont(new Font("Sans-serif", Font.BOLD, 14));
        curUrl.setHorizontalAlignment(SwingConstants.CENTER);
        curUrl.setVisible(true);

        start.addActionListener((ActionEvent e) -> {
            if (start.isEnabled()) {
                start.setEnabled(false);
                startPressed = true;
            }
        });
        start.setPreferredSize(new Dimension(60, progressBar.getHeight()));

        panel = new JPanel(new BorderLayout());
        panel.setSize(new Dimension
                (mainframe.getWidth(), progressBar.getHeight()));
        panel.add(start, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.EAST);
        panel.setVisible(true);
        mainframe.add(panel, BorderLayout.NORTH);
        mainframe.add(slider, BorderLayout.WEST);
        mainframe.getContentPane().add(scroll, BorderLayout.CENTER);
        mainframe.add(curUrl, BorderLayout.SOUTH);
        mainframe.setVisible(true);
        mainframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static boolean handleDialogs() {
        final int   APPROVE = JFileChooser.APPROVE_OPTION;
        int         amt;
        String      exportDir;
        String[]    dates;

        amt = slider.getValue();
        amt = (amt > 0 && amt <= 8) ? amt : 7;
        dates = getDates(amt);
        if (urlsChooser.showOpenDialog(openBtn) == APPROVE &&
            fileChooser.showSaveDialog(saveBtn) == APPROVE) {
            mainframe.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    try {
                        storeProps(fileChooser.getSelectedFile().getAbsolutePath(),
                                urlsChooser.getSelectedFile().getAbsolutePath());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    sysExit(exitCodes.OK.ordinal());
                }
            });
            exportDir = fileChooser.getSelectedFile().getAbsolutePath();
            exportDir = exportDir.substring(0, exportDir.lastIndexOf(slash) + 1);
            outputFiles = getFileNames(exportDir, dates);
            return true;
        }
        else
            return false;
    }

    private static boolean parseWeather(WeatherParser wp) {
        boolean         success;
        String          urlsFile;

        urlsFile = urlsChooser.getSelectedFile().getAbsolutePath();
        textArea.setText("Parsing has started...\n");
        success = wp.parse(outputFiles, urlsFile);
        if (wp.connections == wp.urls.size()) {
            mainframe.setTitle(mainframe.getTitle() + " - " +
                (success ? "Parsing finished" : "Finished with errors"));
            curUrl.setText(success ?
                "Done" : "Finished with errors (see log file)");
            textArea.append(success ? "No errors occurred\n" : "\n");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    static void showErrMsg(Exception e, String data) {
        final String    excName = e.getClass().getSimpleName();
        String          title;

        if ((title = mainframe.getTitle()) != null)
            title += " - " + excName;
        else
            title = excName;
        System.out.println(title + System.lineSeparator() + data.trim());
        scrollDown(textArea);
    }

    @SuppressWarnings("all")
    public static void scrollDown(JTextArea textArea) {
        textArea.setCaretPosition(textArea.getText().length());
    }

    private static void sysExit(int exitCode) {
        System.exit(exitCode);
    }

    private static void storeProps(String outPath, String inPath) {
        Properties p;

        outPath = outPath.substring(0, outPath.lastIndexOf(slash));
        p = new Properties();
        p.setProperty("lastDir", outPath + slash);
        p.setProperty("urlDir", inPath);
        p.setProperty("slider", String.valueOf(slider.getValue()));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(propFilename))) {
            p.store(bw, "Last used folders and properties for " + mainframe.getTitle());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void restoreProps(JFileChooser fileChooser, String outName, JFileChooser urlsChooser) {
        Properties  p;
        String      sValue;
        int         sliderValue;
        File        exportFile;
        File        urlsFile;

        p = new Properties();
        try (BufferedReader br = new BufferedReader(new FileReader(propFilename))) {
            p.load(br);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        exportFile = new File(p.getProperty("lastDir") + outName);
        urlsFile = new File(p.getProperty("urlDir"));
        if ((sValue = p.getProperty("slider")) != null) {
            sliderValue = Integer.parseInt(sValue);
            slider.setValue(sliderValue);
        }
        fileChooser.setSelectedFile(exportFile);
        urlsChooser.setSelectedFile(urlsFile);
    }

    private static String[] getDates(int size) {
        String[]            datesArray;
        Date                date;
        SimpleDateFormat    sdf;
        Calendar            cal;

        datesArray = new String[size];
        date = new Date();
        sdf = new SimpleDateFormat("dd_MM_yyyy");
        cal = Calendar.getInstance();
        cal.setTime(date);
        for (int i = 0; i < size; i++) {
            cal.add(Calendar.DATE, 1); // Adding one day
            date = cal.getTime();
            datesArray[i] = sdf.format(date);
        }
        return datesArray;
    }

    private static String[] getFileNames(String exportDir, String[] dates) {
        final int   size = dates.length;
        String[]    names;

        names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = exportDir.concat("weather_" + dates[i] + ".json");
        }
        return names;
    }

    private enum exitCodes {
        OK,
        ERROR
    }

    public static void main(String[] args) {
        File        jsonFile;
        File        optionsFile;
        String      defaultName;

        initJFrame();
        defaultName = title.replaceAll("\\s+","_") + "_" + getDates(1)[0] + fileExt;
        jsonFile = new File(defaultName);
        optionsFile = new File(propFilename);
        if (optionsFile.exists()) {
            restoreProps(fileChooser, defaultName, urlsChooser);
        } else {
            fileChooser.setSelectedFile(jsonFile);
        }
        textArea.setText("Set the amount of days and press \"Start\"\n");
        while (!startPressed) {
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!handleDialogs())
            sysExit(exitCodes.ERROR.ordinal());
        if (parseWeather(new WeatherParser()))
            mainframe.dispatchEvent(new WindowEvent(mainframe, WindowEvent.WINDOW_CLOSING));
    }
}