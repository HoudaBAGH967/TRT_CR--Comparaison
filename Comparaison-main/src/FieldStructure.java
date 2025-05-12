public class FieldStructure {
    String sortie;
    String champ;
    String type_champ;
    String libelle;
    String type;
    int position;
    int taille;
    String key;

    public FieldStructure(String sortie, String champ,String type_champ, String libelle, String type, int position, int taille) {
        this.sortie = sortie;
        this.champ = champ;
        this.type_champ = type_champ;
        this.libelle = libelle;
        this.type = type;
        this.position = position;
        this.taille = taille;
        
    }

    public String extract(String line,int entet) {
        try {
            int end = Math.min((position-entet) + taille, line.length());
            return line.substring((position-entet), end).trim();
        }catch (Exception e){
//            System.out.println("the error is :");
//            System.out.println("position :"+position);s
//            System.out.println("entet :"+entet);
//            System.out.println("taille :"+taille);
//            System.out.println("line.length() :"+line.length());
//            System.out.println("(position-entet) + taille :"+((position-entet) + taille));
//            
//            System.out.println("(Key  :"+key);
//            System.out.println( e.getMessage());
            return "";
            
        }

    }
}
