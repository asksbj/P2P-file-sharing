
import java.lang.StringBuilder;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.lang.String;


public class logs{

    public static String logmsg(int type, int peer1, int peer2){
        StringBuilder line = new StringBuilder();
        line.append(curtime());
        switch(type){
            case 0:
                line.append("Peer ");
                line.append(peer1);
                line.append(" makes a connection to Peer ");
                line.append(peer2);
                line.append(".\n");
                break;
            case 2:
                line.append("Peer ");
                line.append(peer1);
                if (peer2 == -1){
                    line.append(" doesn't have optimistically unchoked neighbour.\n");
                }else{
                    line.append(" has the optimistically unchoked neighbor ");
                    line.append(peer2);
                    line.append(".\n");
                }
                break;
            case 3:
                line.append("Peer ");
                line.append(peer1);
                line.append(" is unchoked by Peer ");
                line.append(peer2);
                line.append(".\n");
                break;
            case 4:
                line.append("Peer ");
                line.append(peer1);
                line.append(" is choked by Peer ");
                line.append(peer2);
                line.append(".\n");
                break;
            case 6:
                line.append("Peer ");
                line.append(peer1);
                line.append(" received the interested message from Peer ");
                line.append(peer2);
                line.append(".\n");
                break;
            case 7:
                line.append("Peer ");
                line.append(peer1);
                line.append(" received the not interested message from Peer ");
                line.append(peer2);
                line.append(".\n");
                break;
            default:
                break;
        }//switch
        System.out.println(line.toString());
        return line.toString();
    }// logmsg(type, peer1, peer2)

    public static String logmsg(int type, int peer, int[] list){
        StringBuilder line = new StringBuilder();
        line.append(curtime());
        line.append("Peer ");
        line.append(peer);
        if (list == null){
            line.append(" has no preferred neighbours.\n");
        }else{
            line.append(" has the preferred neighbors ");
            for (int i = 0; i<list.length; i++){
                if (i == list.length-1){
                    line.append(list[i]);
                }else{
                    line.append(list[i]);
                    line.append(", ");
                }
            }
            line.append(".\n");
        }
        System.out.println(line.toString());
        return line.toString();
    }
    
    public static String logmsg(int type, int peer){
        StringBuilder line = new StringBuilder();
        line.append(curtime());
        switch(type){
            case 9:
                line.append("Peer ");
                line.append(peer);
                line.append(" has downloaded the complete file.\n");
                break;
            default:
                break;
        }//switch
        System.out.println(line.toString());
        return line.toString();
    }// logmsg(type, peer)
    
    public static String logmsg(int type, int peer1, int peer2, int index){
        StringBuilder line = new StringBuilder();
        line.append(curtime());
        switch(type){
            case 5:
                line.append("Peer ");
                line.append(peer1);
                line.append(" received the have message from Peer ");
                line.append(peer2);
                line.append(" for the piece ");
                line.append(index);
                line.append(".\n");
                break;
            default:
                break;
        }//switch
        System.out.println(line.toString());
        return line.toString();
    }// logmsg(type, peer1, peer2, index)

    public static String logmsg(int type, int peer1, int peer2, int index, int haspieces){
        StringBuilder line = new StringBuilder();
        line.append(curtime());
        switch(type){
            case 8:
                line.append("Peer ");
                line.append(peer1);
                line.append(" has downloaded the piece ");
                line.append(index);
                line.append(" from Peer ");
                line.append(peer2);
                line.append(". Now the nmber of pieces it has is ");
                line.append(haspieces);
                line.append(".\n");
                break;
            default:
                break;
        }//switch
        System.out.println(line.toString());
        return line.toString();
    }// logmsg(type, peer1, peer2, index, pieces)

    private static String curtime(){
        StringBuilder line = new StringBuilder();
        DateFormat timeformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date cur = new Date();
        line.append("[");
        line.append(timeformat.format(cur));
        line.append("]: ");
        return line.toString();
    }// curtime
}// logs
