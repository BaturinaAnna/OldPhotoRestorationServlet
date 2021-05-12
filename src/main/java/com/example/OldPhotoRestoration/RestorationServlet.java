package com.example.OldPhotoRestoration;

import java.io.*;
import javax.servlet.annotation.*;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "restorationServlet", value = "/restoration-servlet")
@MultipartConfig
public class RestorationServlet extends HttpServlet {
    private final ProcessBuilder processBuilderRestoration = new ProcessBuilder("/home/anna/anaconda3/bin/python",
            "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/run.py",
            "--input_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/input",
            "--output_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output"
//            ,--with_scratch").inheritIO();
    ).inheritIO();
    private final String photoPath="/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/input/photoToRestore.jpg";
    private final Map<String, Integer> photoSize = new HashMap<String, Integer>(){{
        put("width", null);
        put("height", null);
    }};

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("START");
        getPhoto(request);
        runPhotoRestorationNN();
        sendPhoto(response);
    }

    private void sendPhoto(HttpServletResponse response) throws IOException {
        System.out.println("Sending");
        String filename = "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/final_output/photoToRestore.png";
        try {
            String mime = "image/*";
            response.setContentType(mime);
            File file = new File(filename);
            response.setContentLength((int) file.length());
            FileInputStream in = new FileInputStream(file);
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[1024];
            int count = 0;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("end sending");
    }

    private void getPhoto(HttpServletRequest request) throws ServletException, IOException {
        System.out.println("get photo");
        InputStream is = request.getPart("file").getInputStream();
        byte[] buffer = new byte[1024];
        FileOutputStream out = new FileOutputStream(photoPath);
        int read;
        while ((read = is.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        is.close();
        out.close();
        System.out.println("Upload");
    }

    private void runPhotoRestorationNN(){
        try {
            System.out.println("run restoration");
            Process p = processBuilderRestoration.start();
            p.waitFor();
            BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = bfr.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();
            p.destroy();
            System.out.println("Restored");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}