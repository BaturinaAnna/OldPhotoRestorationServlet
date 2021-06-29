package com.example.OldPhotoRestoration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "restorationServlet", value = "/restoration-servlet")
@MultipartConfig
public class RestorationServlet extends HttpServlet {
    private final ProcessBuilder processBuilderRestorationWithoutScratches = new ProcessBuilder("/home/anna/anaconda3/bin/python",
            "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/run.py",
            "--input_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/input",
            "--output_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output"
            ).inheritIO();
    private final ProcessBuilder processBuilderRestorationWithScratches = new ProcessBuilder("/home/anna/anaconda3/bin/python",
            "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/run.py",
            "--input_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/input",
            "--output_folder", "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output",
            "--with_scratch").inheritIO();

    private final String photoPath="/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/input/photoToRestore.jpg";
    private String removeScratches;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("START");
        getPhoto(request);
        cleanOutputDirectory();
        runPhotoRestorationNN();
        sendPhoto(response);
    }

    private void getPhoto(HttpServletRequest request) throws ServletException, IOException {
        System.out.println("get photo");

        removeScratches = new BufferedReader(new InputStreamReader(request.getPart("removeScratches").getInputStream()))
                .lines().collect(Collectors.joining("\n"));

        InputStream is = request.getPart("photoToRestore").getInputStream();
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
            Process p;
            if (removeScratches.equals("true")) {
                p = processBuilderRestorationWithScratches.start();
            } else {
                p = processBuilderRestorationWithoutScratches.start();
            }
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

    private void cleanOutputDirectory() throws IOException {
        System.out.println("Start cleaning");
        FileUtils.deleteDirectory(new File("/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/final_output"));
        FileUtils.deleteDirectory(new File("/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/stage_1_restore_output"));
        FileUtils.deleteDirectory(new File("/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/stage_2_detection_output"));
        FileUtils.deleteDirectory(new File("/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/stage_3_face_output"));
        System.out.println("Finish cleaning");
    }

    private void sendPhoto(HttpServletResponse response) throws IOException {
        System.out.println("Sending");
        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"photoArchive.zip\"");

        Collection<File> filesToSend = new ArrayList<File>();
        String pathToFiles = "/home/anna/Документы/AndroidProject/Bringing-Old-Photos-Back-to-Life/output/";

        filesToSend.add(new File(pathToFiles + "final_output/photoToRestore.png"));

        String[] faces = new File(pathToFiles + "stage_3_face_output/each_img").list();
        if (faces.length != 0){
            for( String face : faces) {
                filesToSend.add(new File(pathToFiles + "stage_3_face_output/each_img/" + face));
                System.out.println(face);
            }
        }

        OutputStream os = null;
        BufferedOutputStream bos = null;

        ZipOutputStream zos = null;

        try {
            os = response.getOutputStream();
            bos = new BufferedOutputStream(os);

            zos = new ZipOutputStream(bos);
            zos.setLevel(ZipOutputStream.STORED);

            sendMultipleFiles(zos, filesToSend);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (zos != null) {
                zos.finish();
                zos.flush();
                IOUtils.closeQuietly(zos);
            }
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
        System.out.println("end sending");
    }

    private void sendMultipleFiles(ZipOutputStream zos, Collection<File> filesToSend) throws IOException {
        for (File f : filesToSend) {

            InputStream inStream = null;
            ZipEntry ze = null;

            try {
                inStream = new FileInputStream(f);

                ze = new ZipEntry(f.getName() + "-archived");

                zos.putNextEntry(ze);

                IOUtils.copy(inStream, zos);
            } catch (IOException e) {
                System.out.println("Cannot find " + f.getAbsolutePath());
            } finally {
                IOUtils.closeQuietly(inStream);
                if (ze != null) {
                    zos.closeEntry();
                }
            }
        }
    }
}