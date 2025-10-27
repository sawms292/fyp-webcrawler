package org.saw.webcrawler.fxfeatures;
import java.io.File;
import java.lang.management.ManagementFactory;

import com.mysql.cj.protocol.x.XMessage;
import com.sun.management.OperatingSystemMXBean;

import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class readInfo {
    public static ArrayList<String> logs = new ArrayList<String>();

    public static String timestamp(){
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return now.format(formatter) + "-> ";
    }
    public boolean sendMessage(String messagess) {
        String joinString = "";
        String result = "";
        if(!logs.isEmpty()){
            //resource from online/chatgpt https://stackoverflow.com/questions/1751844/java-convert-liststring-to-a-joind-string
             joinString = logs.stream()
                    .map(log -> "<li>" + log + "</li>")  // Wrap each log with <li> tags
                    .collect(Collectors.joining("<br>"));  // Join with <br> (optional, depending on desired formatting)

            result = "<ol>" + joinString + "</ol>";  // Wrap the entire string in <ol> tags
            System.out.println(result);
        }
        final String fromemail ="sheng00.ms@gmail.com";
        final String password ="xqpc sgue piyo gati";
        final String toemail ="sheng000.ms@gmail.com";
        String messages = messagess + systemInfo() + "\nBelow is logs:\n" + result;
        System.out.println("Email Start" + System.currentTimeMillis());
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromemail, password);
            }
        };
        Session session = Session.getInstance(props, auth);
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromemail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toemail));
            message.setSubject("Check Bug Report");
            message.setContent(messages, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("readInfo file: Email Sent" + System.currentTimeMillis());
            logs.clear();
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String systemInfo(){
        File logFile;
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int cpu = os.getAvailableProcessors();
        long totalMemory, freeMemory, usedMemory, totalStorage, freeStorage, usedStorage;
        int mb = 1024 * 1024;
        int gb = 1024 * 1024 * 1024;
        File diskPartition = new File("C:");
        totalMemory = os.getTotalPhysicalMemorySize();
        freeMemory = os.getFreePhysicalMemorySize();
        usedMemory = totalMemory - freeMemory;
        totalStorage = diskPartition.getTotalSpace();
        freeStorage = diskPartition.getFreeSpace();
        usedStorage = totalStorage - freeStorage;
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>User Computer Information</title></head>")
                .append("<body>")
                //User Operating System
                .append("<table border='1' style='border-collapse:collapse;'>")
                .append("<thead><tr>")
                .append("<th colspan='3' style='text-align:center;'>Operating System Information</th>")
                .append("</tr></thead>")
                .append("<thead><tr>")
                .append("<th>Operating system</th>")
                .append("<th>Operating system version</th>")
                .append("<th>Operating system architecture</th>")
                .append("</tr><thead>")
                .append("<tbody><tr>")
                .append("<td>" + System.getProperty("os.name") + "</td>")
                .append("<td>"+ System.getProperty("os.version") + "</td>")
                .append("<td>" + System.getProperty("os.arch") + "</td>")
                .append("</tr></tbody>")
                .append("</table>")
                //End of Operating System Information
                .append("<br>")
                //User CPU, Memory, Storage
                .append("<table border='1' style='border-collapse:collapse;'>")
                .append("<thead><tr>")
                .append("<th colspan='7' style=';text-align:center'>CPU, Memory, Storage Usage</th>")
                .append("</tr></thead>")
                .append("<thead><tr>")
                .append("<th>Core of CPU</th>")
                .append("<th>Total Memory</th>")
                .append("<th>Used Memory</th>")
                .append("<th>Free Memory</th>")
                .append("<th>Total Storage</th>")
                .append("<th>Used Storage</th>")
                .append("<th>Free Storage</th>")
                .append("</tr></thead>")
                .append("<tbody><tr>")
                .append("<td>" + cpu + "</td>")
                .append("<td>" + totalMemory / mb + "MB" + "</td>")
                .append("<td>" + usedMemory / mb + "MB" + "</td>")
                .append("<td>" + freeMemory / mb + "MB" + "</td>")
                .append("<td>" + totalStorage / gb + "GB" + "</td>")
                .append("<td>" + usedStorage / gb + "GB" + "</td>")
                .append("<td>" + freeStorage / gb + "GB" + "</td>")
                .append("</tr></tbody>")
                .append("</table>")
                //End of CPU, Memory, Storage Information
                .append("<br>")
                //User Directory
                .append("<table border='1' style='border-collapse:collapse;'>")
                .append("<thead><tr>")
                .append("<th colspan='2' style='text-align:center;'>Computer Directory</th>")
                .append("</tr></thead>")
                .append("<thead><tr>")
                .append("<th>User Directory</th>")
                .append("<th>Application Install Directory</th>")
                .append("</tr></thead>")
                .append("<tbody><tr>")
                .append("<td>" + System.getProperty("user.home") + "</td>")
                .append("<td>" + System.getProperty("user.dir") + "</td>")
                .append("</tr></tbody>")
                .append("</table>")
                //End of User Directory
                .append("</body></html>");
        return sb.toString();
    }

}
