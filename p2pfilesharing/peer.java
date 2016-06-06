
import java.lang.Math;
public class peer {
	public int[] bitfield=null;
	public int[] checked=null;
	public int pieces;
	public int size;
	public int selfid;
	public int prefix;
	public int id;
	public String host;
	public int port;
	public int haveall;
	
	public peer(int pid,String phost,int pport,int phave){
		id=pid;
		host=phost;
		port=pport;
		haveall=phave;
		bitfield=new int[peerProcess.Pieces];
		checked=new int[peerProcess.Pieces];
		for(int i=0;i<bitfield.length;i++){
			if(phave==1){
				bitfield[i]=1;
				checked[i]=1;
			}else{
				bitfield[i]=0;
				checked[i]=0;
			}
		}
	}
	
	public boolean have(int ind){
		if(bitfield[ind]==1){
			return true;
		}else{
			return false;
		}
	}
	
	public int HaveHowMany(){
		int result=0;
		for(int i=0;i<bitfield.length;i++){
			result+=bitfield[i];
		}
		return result;
	}
	
	synchronized public void set(int ind){
		bitfield[ind]=1;
		if(gotAll()==true){
			haveall=1;
		}
	}
	
	public boolean gotAll(){
		if(haveall==1){
			return true;
		}
		for(int i=0;i<peerProcess.Pieces;i++){
			if(bitfield[i]==0){
				return false;
			}
		}
		return true;
	}
	
	
	synchronized public void setBitfield(String field){
		if(field.length()!=bitfield.length){
			System.out.println("The length of field input must equal to the number of pieces");
			return;
		}
		for(int i=0;i<field.length();i++){
			if(field.charAt(i)=='1'){
				bitfield[i]=1;
			}else{
				bitfield[i]=0;
			}
		}
	}
	
	public String getBitfield(){
		StringBuilder field=new StringBuilder();
		for(int i=0;i<bitfield.length;i++){
			field.append(bitfield[i]);
		}
		return field.toString();
	}
	
	synchronized public void setAll(){
		haveall=1;
		for(int i=0;i<pieces;i++){
			bitfield[i]=1;
		}
	}
	
	public int interest(int otherid){
		int blocksize=peerProcess.Pieces/(peerProcess.Peers.size()-1)+1;
		int offset=blocksize*(peerProcess.SelfID%peerProcess.Prefix-1);
		int result=-1;
		for (int i=0;i<peerProcess.Pieces;i++){
			int ind=(i+offset)%peerProcess.Pieces;
			if(peerProcess.Peers.elementAt(otherid%peerProcess.Prefix-1).have(ind)==true&&bitfield[ind]==0&&checked[ind]==0){
				checked[ind]=1;
				return ind;
			}
		}
		return result;
	}

}
