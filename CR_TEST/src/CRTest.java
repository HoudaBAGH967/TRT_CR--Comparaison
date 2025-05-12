import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CRTest {

    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Usage :  <Fichier1> <Fichier2> <Fichier3> <Fichier4> <Fichier5> <FichierOutput> ");
            return;
        }

        String[] fichiers = new String[6];
        for (int i = 0; i <6; i++) {
            fichiers[i] = args[i];
        }
        String fichierOutput = args[6];
        // Traitement des fichiers
        Set<String> lignesConcat = new LinkedHashSet<>();
        String[] headers = {"NB lignes CRE", "NB lignes discard", "NB rejet", "NB ME", "NB ME Agrégé"};
        long[] lignesFichier = new long[fichiers.length];

        for (int i = 0; i < fichiers.length; i++) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(fichiers[i]))) {
                String ligne;
                long count = 0;
                while ((ligne = reader.readLine()) != null) {
                    if (ligne.trim().isEmpty()) continue;
                    count++;

                    // Extraire les données uniquement pour les fichiers 1, 4, et 5 (index 1, 4, 5)
                    if ((i == 1 || i == 4 || i == 5) && ligne.length() >= 98) {
                        String cdReg = ligne.substring(65, 72).trim();
                        String schema = ligne.substring(72, 81).trim();
                        String dest = ligne.substring(0, 10).trim();
                        String ecr = ligne.substring(89, 97).trim();
                        lignesConcat.add(String.join(";", cdReg, schema, dest, ecr));
                    }
                }
                lignesFichier[i] = count;
            } catch (IOException e) {
                System.out.println("Fichier introuvable : " + fichiers[i] + " - " + e.getMessage());
            }
        }

        

        // Écriture dans Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Results");
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            sheet.createRow(2).createCell(0).setCellValue("Nom de fichier en entree : " + new File(fichiers[0]).getName());

            Row headerRow = sheet.createRow(5);
            Row dataRow = sheet.createRow(6);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(borderStyle);

                Cell valueCell = dataRow.createCell(i);
                if ("NB ME Agrégé".equals(headers[i])) {
                    valueCell.setCellValue(lignesFichier[4] + lignesFichier[5]);
                } else {
                    valueCell.setCellValue(lignesFichier[i]);
                }
                valueCell.setCellStyle(borderStyle);
                sheet.autoSizeColumn(i);
            }

            Row headerDistinct = sheet.createRow(9);
            String[] distinctHeaders = {"CD REGL", "SCHEMA", "DESTINATAIRE", "ECR"};
            for (int i = 0; i < distinctHeaders.length; i++) {
                Cell cell = headerDistinct.createCell(i);
                cell.setCellValue(distinctHeaders[i]);
                cell.setCellStyle(borderStyle);
                sheet.autoSizeColumn(i);
            }

            int rowIdx = 10;
            for (String ligne : lignesConcat) {
                Row row = sheet.createRow(rowIdx++);
                String[] valeurs = ligne.split(";");
                for (int i = 0; i < valeurs.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(valeurs[i]);
                    cell.setCellStyle(borderStyle);
                    sheet.autoSizeColumn(i);
                }
            }

            try (FileOutputStream out = new FileOutputStream(fichierOutput)) {
                workbook.write(out);
                System.out.println("Fichier Excel généré : " + fichierOutput);
            }

        } catch (IOException e) {
            System.out.println("Erreur génération Excel : " + e.getMessage());
        }
    }
}
