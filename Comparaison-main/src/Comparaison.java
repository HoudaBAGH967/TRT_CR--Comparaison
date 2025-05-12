import java.io.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.text.SimpleDateFormat;
import java.util.*;

public class Comparaison {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("USAGE: java Main <file1Path> <file2Path> <structurePath> <outputPath> <structureName> <Seuil de comparaison>");
            System.exit(1);
        }

        String file1Path = args[0];
        String file2Path = args[1];
        String structurePath = args[2];
        String outputPath = args[3];
        String structureName = args[4];
        int seuilComparaison = Integer.parseInt(args[5]);

        String fileName1 = new File(file1Path).getName();
        String fileName2 = new File(file2Path).getName();

        Map<String, List<FieldStructure>> structures = readStructureFromCSV(structurePath);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Comparaison");

        String[] headers = {
            "Date heure", "ME", "Code destinataire", "Identifiant de compostage",
            "Regle d agrégation", "Regle d équilibre", "Régle d'audit", "Nom de l'interface",
            "Nom du schéma", "Nom de l'écriture", "Ligne_ISIE", "Ligne_AFAH",
            "Colonne", "Libelle", "Position", "Taille", "Type", "Valeur_ISIE", "Valeur_AFAH"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String formattedDate = formatter.format(new Date());

        try (
            BufferedReader reader1 = new BufferedReader(new FileReader(file1Path));
            BufferedReader reader2 = new BufferedReader(new FileReader(file2Path))
        ) {
            int lineNum1 = 1, lineNum2 = 1;
            String line1 = reader1.readLine();
            String line2 = reader2.readLine();

            while (line1 != null || line2 != null) {
                ComparaisonResult result = writeComparaisonToXLSX(
                    formattedDate, line1, line2,
                    structures.get(structureName),
                    sheet.getLastRowNum() + 1,
                    lineNum1, lineNum2, sheet,fileName1,fileName2, seuilComparaison, structureName
                );

                sheet = result.sheet;
                int comparaisonResult = result.results;
             if(line1 != null && line2 != null){
                if (comparaisonResult == 0) {
                    lineNum1++;
                    lineNum2++;
                    line1 = reader1.readLine();
                    line2 = reader2.readLine();
                } else if (comparaisonResult == 1) {
                    lineNum2++;
                    line2 = reader2.readLine();
                } else if (comparaisonResult == 2) {
                    lineNum1++;
                    line1 = reader1.readLine();
                }
            }
                if (line1 == null && line2 != null) {
                	 while (line2 != null) {
                		 writeEndMessage(sheet, "Ligne " + lineNum2 +" du fichier  "+ fileName2 +"  n'existe pas dans fichier  "+ fileName1+"  :{ " +line2.substring(191)+" }");
                	        lineNum2++;
                	        line2 = reader2.readLine();
                	    }
                	    break;
                } else if (line2 == null && line1 != null) {
                	 while (line1 != null) {
                	        writeEndMessage(sheet, "Ligne " + lineNum1 +" du fichier  "+ fileName1 +"  n'existe pas dans fichier  "+ fileName2+"  :{ " +line1+" }");
                	        lineNum1++;
                	        line1 = reader1.readLine();
                	    }
                	    break;
                    
                }
            }

            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                workbook.write(out);
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeEndMessage(Sheet sheet, String message) {
        Row endRow = sheet.createRow(sheet.getLastRowNum() + 1);
        endRow.createCell(0).setCellValue(message);
    }

    public static Map<String, List<FieldStructure>> readStructureFromCSV(String path) {
        Map<String, List<FieldStructure>> structures = null;
        try { 
            structures = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            boolean isHeader = true;
            List<FieldStructure> structure = new ArrayList<>();
            String lastStructure = null;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                } // Skip header
                String[] parts = line.split(";", -1); // Split on semicolon
                if (parts.length >= 7) {
                    String sortie = parts[0].trim();
                    String type_champ = parts[1].trim();
                    String champ = parts[2].trim();
                    String libelle = parts[3].trim();
                    String type = parts[4].trim();
                    int position = Integer.parseInt(parts[5].trim());
                    int taille = Integer.parseInt(parts[6].trim());

                    if (lastStructure == null) {
                        lastStructure = sortie.trim();
                    } else if (!lastStructure.equals(sortie.trim())) {
                        structures.put(lastStructure, structure);
                        structure = new ArrayList<>();
                        lastStructure = sortie.trim();
                    }
                    // Subtract 1 from position if 1-based indexing in CSV
                    structure.add(new FieldStructure(sortie, champ, type_champ, libelle, type, position - 1, taille));
                }
            }
            structures.put(lastStructure, structure);
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return structures;
    }
  
    
    
    public static class ComparaisonResult {
        public Sheet sheet;
        public int results;

        public ComparaisonResult(Sheet sheet, int results) {
            this.sheet = sheet;
            this.results = results;
        }
    }
    
    public static ComparaisonResult writeComparaisonToXLSX(String formattedDate, String file1Lines, String file2Lines,
            List<FieldStructure> structure, int rowNum, int line1Index, int line2Index, Sheet sheet,String fileName1,String fileName2, int seuilComparaison,String structureName) throws IOException {

        int entet = 0;
        int diffCount = 0;
        List<Object[]> differences = new ArrayList<>();
        String file2Line = "";
        String[] champsCriteres;
       	if (structureName.equals("ACTIF")) {
        	    champsCriteres = new String[]{
        	        "CODE_ENTRE", "CODE_ETABL", "CODE_APPLI", "CODE_DEVIS",
        	        "CODE_JOURN", "DATE_COMPT", "MONTANT_SI", "SENS_NORMA",
        	        "LIBELLE_1", "CONTREVAL", "LIBELL2", "DATE_DENOU", "DATE_OPERA"
        	    };
        	}else {
        		champsCriteres = new String[]{
            	        "COCENT", "COETAJ", "CCARTE", "COJOUR",
            	        "DATECR", "NOFOLI", "NOLIFO", "NOPIEC",
            	        "COTYMV", "COSPAI", "DATECI", "COGNAL", "COSENS", "ZX017_VDMT", "CODVIS", "COFORM", "FILLER_8", "DATVAL", "FILLER_2_1", "TTYOP", "TNAOP", "TCOSHP", "FILLER_1_1", "COBONP", "CODOPE", "DATACQ", "COTRBA", "CONATU"};}
        Map<String, String> vals1 = new HashMap<>();
        Map<String, String> vals2 = new HashMap<>();
        boolean test = false;

        String CDDEST = "";
        String IDCOMP = "";
        String RGAGREG = "";
        String RGEQUI = "";
        String RGAUDIT = "";
        String NMINTERFAC = "";
        String NMSCHEMA = "";
        String NMECRITURE = "";

        for (FieldStructure field : structure) {
            if (field.type_champ.equals("ENTETE")) {
                entet = field.position + field.taille;
                switch (field.champ) {
                    case "CDDEST": CDDEST = field.extract(file2Lines, 0); break;
                    case "IDCOMP": IDCOMP = field.extract(file2Lines, 0); break;
                    case "RGAGREG": RGAGREG = field.extract(file2Lines, 0); break;
                    case "RGEQUI": RGEQUI = field.extract(file2Lines, 0); break;
                    case "RGAUDIT": RGAUDIT = field.extract(file2Lines, 0); break;
                    case "NMINTERFAC": NMINTERFAC = field.extract(file2Lines, 0); break;
                    case "NMSCHEMA": NMSCHEMA = field.extract(file2Lines, 0); break;
                    case "NMECRITURE": NMECRITURE = field.extract(file2Lines, 0); break;
                }
                continue;
            } else {
                test = true;
            }

            if (test) {
                file2Line = file2Lines.substring(entet, file2Lines.length() - 1);
                test = false;
            }

            String val1 = field.extract(file1Lines, entet);
            String val2 = field.extract(file2Line, entet);

            if (!val1.equals(val2)) {
                diffCount++;
                differences.add(new Object[]{
                    formattedDate, field.sortie, CDDEST, IDCOMP, RGAGREG, RGEQUI, RGAUDIT, NMINTERFAC, NMSCHEMA, NMECRITURE,
                    line1Index, line2Index, field.champ, field.libelle, field.position + 1, field.taille, field.type, val1, val2
                });

                for (String critere : champsCriteres) {
                    if (field.champ.equals(critere)) {
                        vals1.put(critere, val1);
                        vals2.put(critere, val2);
                    }
                }
            }
        }
      
        int results = 0;
        if (diffCount > seuilComparaison) {
            Row row = sheet.createRow(rowNum++);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 34));

            for (String critere : champsCriteres) {
                String v1 = vals1.getOrDefault(critere, "");
                String v2 = vals2.getOrDefault(critere, "");

                int comp = v1.compareTo(v2);
                if (comp != 0) {
                    if (comp > 0) {
                        results = 1;
                        row.createCell(0).setCellValue("Ligne " + line2Index +" du fichier  "+ fileName2 +"  n'existe pas dans fichier  "+ fileName1+"  :{ " +file2Lines.substring(191)+" }");
                        
                    } else {
                        results = 2;
                        row.createCell(0).setCellValue("Ligne " + line1Index +" du fichier  "+ fileName1 +"  n'existe pas dans fichier  "+ fileName2+"  :{ " +file1Lines+" }");
                        
                       
                    }
                    break;
                }
            }
        } else {
        	results = 0;
            for (Object[] diff : differences) {
                Row row1 = sheet.createRow(rowNum++);
                for (int i = 0; i < diff.length; i++) {
                    if (diff[i] != null) row1.createCell(i).setCellValue(diff[i].toString());
                    sheet.autoSizeColumn(i);
                }
            }
        }

        return new ComparaisonResult(sheet, results);
    }
}