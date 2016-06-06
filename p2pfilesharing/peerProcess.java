
import java.util.*;
import java.lang.*;
import java.io.*;
import java.lang.Math.*;
import java.net.*;
import java.lang.Thread;
import java.util.Random;

import javax.swing.JFrame;

public class peerProcess {
	public static String FileName;
	public static int FileSize;
	public static int NumberOfPreferredNeighbours;
	public static int UnchokingInterval;
	public static int OptimisticUnchokingInterval;
	public static int PieceSize;
	public static int Pieces;
	
	public static Vector<peer> Peers=new Vector<>();
	public static String Localip;
	public static int SelfID;
	public static int Prefix;
	public static byte[] data;
	public static boolean[] choked;
	public static FileWriter logfile=null;
	public static Vector<process> Processes=new Vector<>();
	public static Vector<Thread> WorkTrd=new Vector<>();
	
	public static int[] Speed;
	public static int[] PreferredNeighbour;
	public static int OptimisticNeighbour;

	public static void main(String[] args) {
		//Initialization
		//JFrame p2pSys=new peertable();
		
		//Localip=peertable.getLocalIp();
		if(args.length==0){
			System.out.println("Please specify an ID for this peer ");
			return;
		}
		SelfID=Integer.parseInt(args[0]);
		Prefix=SelfID-SelfID%1000;
		try{
			System.out.println("Peer "+SelfID+" is initializing...");
			if(init()==false){
				System.out.println("Initialization failed!");
				return;
			}
		}catch(Exception e){
		}
		System.out.println("Done.");
		
		//Establish TCP connections
		//1.extablish a server socket to listen if it is not the last one in peer list
		ServerSocket listenSocket=null;
		if(SelfID!=Peers.lastElement().id){
			try{
				System.out.print("Peer "+SelfID+" is openning listening socket...");
				listenSocket=new ServerSocket(Peers.elementAt(SelfID%1000-1).port);
			}catch(Exception e){
				System.out.println("Peer "+SelfID+" can't open listening socket.");
				return;
			}
			System.out.println("Done.");
		}
		
		//2.establish connection to all previous peers
		Socket toPrevious;
		Date cur=null;
		for(int i=0;i<SelfID%1000-1;i++){
			try{
				logfile.write(logs.logmsg(0,SelfID,Peers.elementAt(i).id));
				System.out.print("Peer "+SelfID+" is making TCP connection to peer "+Peers.elementAt(i).id+"...");
				toPrevious=new Socket(Peers.elementAt(i).host,Peers.elementAt(i).port);
				Processes.add(new process(toPrevious, Peers.elementAt(i).id));
				Thread trd = new Thread(Processes.lastElement());
				trd.setPriority(10);
				WorkTrd.add(trd);
			}catch(Exception e){
				System.out.print("Peer "+SelfID+" can't make TCP connection to Peer "+Peers.elementAt(i).id);
				for(int j=0;j<Processes.size();j++){
					try{
						Processes.elementAt(j).processSocket.close();
					}catch(Exception e1){}
				}
				return;
			}
			System.out.println("Done.");
		}
		
		//3.accept connections from later peers
		if(listenSocket!=null){
			Socket toLater;
			System.out.println("Peer "+SelfID+" is listening for later peers...");
			int laterPeers=Peers.lastElement().id-SelfID;
			
			long prev=System.currentTimeMillis();
			long curr=prev;
			try{
				listenSocket.setSoTimeout(10000);
			}catch(Exception e){}
			while(laterPeers>0){
				prev=curr;
				curr=System.currentTimeMillis();
				try{
					toLater=listenSocket.accept();
					Processes.add(new process(toLater));
					Thread trd=new Thread(Processes.lastElement());
					trd.setPriority(10);
					WorkTrd.add(trd);
					
				}catch(Exception e){
					System.out.println("Peer "+SelfID+" can't receive TCP requests from later peers");
					return;
				}
				laterPeers--;
			}
			if(laterPeers>0){
				//failed
				for(int j=0;j<Processes.size();j++){
					try{
						Processes.elementAt(j).processSocket.close();
					}catch(Exception e1){}
				}
				return;
			}
			System.out.println("Peer "+SelfID+"has established TCP connections with all peers.");
			try{
				listenSocket.close();
			}catch(Exception e){
				return;
			}
		}
		
		//established all TCP connections, start threads now
		for(int i=0;i<WorkTrd.size();i++){
			WorkTrd.elementAt(i).start();
		}
		long prevPref=System.currentTimeMillis();
		long prevOpti=prevPref;
		long curtime;
		while(allHave()==false){
			curtime=System.currentTimeMillis();
			if(curtime-prevPref>UnchokingInterval*1000){
				//Time to reselect preffered neighbours
				PreferredNeighbour=selectPreferred();
				prevPref=curtime;
			}
			if(curtime-prevOpti>peerProcess.OptimisticUnchokingInterval*1000){
				//Time to reselect optimistic neighbour
				OptimisticNeighbour=selectOptimistic();
				prevOpti=curtime;
			}
			try{
				Thread.sleep(100);
			}catch(Exception e){}
		}
		
		//all peers have all the pieces
		//store the file on the disk and exit
		if(SelfID!=Peers.elementAt(0).id){
			StringBuilder filenm=new StringBuilder();
			filenm.append("./peer_");
			filenm.append(SelfID);
			filenm.append("/");
			filenm.append(FileName);
			try{
				FileOutputStream out =new FileOutputStream(filenm.toString());
				out.write(data);
				out.close();
			}catch(Exception e){}
		}
		
		//The procsss threads exist
		for(int i=0;i<WorkTrd.size();i++){
			try{
				WorkTrd.elementAt(i).join();
			}catch(Exception e){}
		}
		
		//All process threads are terminated. Teminate self.
		try{
			logfile.close();
		}catch(Exception e){}
		for(int j=0;j<Processes.size();j++){
			try{
				Processes.elementAt(j).processSocket.close();
			}catch(Exception e1){}
		}
		System.out.println("Peer "+SelfID+" is exiting now.");
		
	}
	
	public static boolean init() throws Exception{
		BufferedReader paracfg=null;
		BufferedReader peercfg=null;
		String para=null;
		String[] str=null;
		boolean success=true;
		try{
			//1.read the common config file.
			paracfg=new BufferedReader(new FileReader(new File("Common.cfg")));
			para=paracfg.readLine();
			str=para.split(" ");
			NumberOfPreferredNeighbours=Integer.parseInt(str[1]);
			para=paracfg.readLine();
			str=para.split(" ");
			UnchokingInterval=Integer.parseInt(str[1]);
			para=paracfg.readLine();
			str=para.split(" ");
			OptimisticUnchokingInterval=Integer.parseInt(str[1]);
			para=paracfg.readLine();
			str=para.split(" ");
			FileName=str[1];
			para=paracfg.readLine();
			str=para.split(" ");
			FileSize=Integer.parseInt(str[1]);
			para=paracfg.readLine();
			str=para.split(" ");
			PieceSize=Integer.parseInt(str[1]);
			Pieces=(int)(Math.ceil((double)FileSize/PieceSize));

			paracfg.close();
			
			//2.read the peer info config file. Generate peers.
			peercfg=new BufferedReader(new FileReader(new File("PeerInfo.cfg")));
			int id;
			String host;
			int port;
			int have;
			
			while((para=peercfg.readLine())!=null){
				str=para.split(" ");
				if(str[0].charAt(0)!='%'){
					id=Integer.parseInt(str[0]);
					host=str[1];
					port=Integer.parseInt(str[2]);
					have=Integer.parseInt(str[3]);
					Peers.add(new peer(id,host,port,have));
					if(have==0){
						String dirName="peer_"+id;
						File dirFile=new File(dirName);
						if(dirFile.exists()==false){
							if(dirFile.mkdir()==false){
								System.out.println("Making directory failed for peer "+id);
								return false;
							}
						}
					}
				}
			}
			peercfg.close();
			//
			//3.get a block of memory for the data,if peer has data,read if from file
			choked=new boolean[Peers.size()];
			for(int i=0;i<Peers.size();i++){
				choked[i]=true;
			}
			data=new byte[FileSize];
			StringBuilder filenm=new StringBuilder();
			int ind=SelfID%1000-1;
			if(Peers.elementAt(ind).haveall==0){
				for(int i=0;i<FileSize;i++){
					data[i]=0;
				}
			}else{

				filenm.append("./peer_");
				filenm.append(SelfID);
				filenm.append("/");
				filenm.append(FileName);
				InputStream in=null;
				int byteread;
				try{
					in=new FileInputStream(filenm.toString());

					if((byteread=in.read(data, 0, FileSize))!=FileSize){
						System.out.println("Read process gets wrong!");
						throw(new Exception("File size is wrong."));
					}
				}catch(Exception ine){
					data=null;
				}finally{
					if(in!=null){
						in.close();
					}
				}
			}
			
			//4.open the log file
			filenm.delete(0, filenm.length());
			filenm.append("log_peer_");
			filenm.append(SelfID);
			filenm.append(".log");
			logfile=new FileWriter(filenm.toString(),false);
			
			//5.initialize neighbour list
			Speed=new int[Peers.size()];
			OptimisticNeighbour=-1;
			PreferredNeighbour=selectPreferred();
			OptimisticNeighbour=selectOptimistic();
			
		}catch(Exception e){
			e.printStackTrace();
			success=false;
			if(logfile!=null){
				try{
					logfile.close();
				}catch(Exception e1){
				}
			}
		}finally{
			if(paracfg!=null){
				try{
					paracfg.close();
				}catch(Exception e){
				}
			}
			if(peercfg!=null){
				try{
					peercfg.close();
				}catch(Exception e){
				}
			}
		}
		
		return success;
	}//init
	
	private static boolean allHave(){
		for(int i=0;i<Peers.size();i++){
			if(Peers.elementAt(i).gotAll()==false){
				return false;
			}
		}
		return true;
	}
	
	public static int[] selectPreferred(){
		Vector<Integer> candidates=new Vector();
		for(int i=0;i<Peers.size();i++){
			candidates.add(i);
		}
		candidates.remove(SelfID%1000-1);
		if(OptimisticNeighbour>0){
			for(int i=0;i<candidates.size();i++){
				if(OptimisticNeighbour%1000-1==candidates.elementAt(i)){
					candidates.remove(i);
					break;
				}
			}
		}
		int j=0;
		while(j<candidates.size()){
			if(Peers.elementAt(candidates.elementAt(j)).gotAll()==true){
				candidates.remove(j);
			}else{
				j++;
			}
		}
		int[] list;
		if(candidates.size()==0){
			list=null;
		}else if(candidates.size()<=NumberOfPreferredNeighbours){
			list=new int[candidates.size()];
			for(int i=0;i<candidates.size();i++){
				list[i]=candidates.elementAt(i)+1+Prefix;
			}
		}else{
			list=new int[NumberOfPreferredNeighbours];
			if(Peers.elementAt(SelfID%Prefix-1).gotAll()==true){//self has the full file
				Random rd=new Random();
				rd.setSeed(System.currentTimeMillis());
				for(int i=0;i<NumberOfPreferredNeighbours;i++){
					int ind=Math.abs(rd.nextInt())%candidates.size();
					list[i]=candidates.elementAt(ind)+1+Prefix;
					candidates.remove(ind);
				}
			}else{//select preferred neighbor according to the speed
				int[] canSpeed=new int[candidates.size()];
				for(int i=0;i<candidates.size();i++){
					canSpeed[i]=Speed[candidates.elementAt(i)];
				}
				int temp,cantemp;
				for(int i=0;i<candidates.size();i++){
					int m=i-1;
					temp=canSpeed[i];
					cantemp=candidates.elementAt(i);
					while(m>=0&&temp>canSpeed[m]){
						canSpeed[m+1]=canSpeed[m];
						canSpeed[m]=temp;
						candidates.set(m+1,candidates.elementAt(m));
						candidates.set(m,cantemp);
						m--;
					}
				}
				for(int i=0;i<NumberOfPreferredNeighbours;i++){
					list[i]=candidates.elementAt(i)+1+Prefix;
				}
			}
		}//select
		
		for(int i=0;i<choked.length;i++){
			choked[i]=true;
		}
		if(list!=null){
			for(int i=0;i<list.length;i++){
				choked[list[i]%Prefix-1]=false;
			}
		}
		if(OptimisticNeighbour!=-1){
			choked[OptimisticNeighbour%Prefix-1]=false;	
		}
		for(int i=0;i<Speed.length;i++){
			Speed[i]=0;
		}
		
		try{
			logfile.write(logs.logmsg(1, SelfID, list));
		}catch(Exception e){
		}
		return list;
	}
	
	public static int selectOptimistic(){
		Vector<Integer> candidates=new Vector();
		for(int i=0;i<Peers.size();i++){
			candidates.add(i);
		}
		candidates.remove(SelfID%1000-1);
		if(PreferredNeighbour!=null){
			for(int i=0;i<PreferredNeighbour.length;i++){
				for(int j=0;j<candidates.size();j++){
					if(PreferredNeighbour[i]%1000-1==candidates.elementAt(j)){
						candidates.remove(j);
						break;
					}
				}
			}
		}
		int m=0;
		while(m<candidates.size()){
			if(Peers.elementAt(candidates.elementAt(m)).gotAll()==true){
				candidates.remove(m);
			}else{
				m++;
			}
		}
		
		int list;
		if(candidates.size()==0){
			list=-1;
		}else{
			Random rd=new Random();
			rd.setSeed(System.currentTimeMillis());
			int ind=Math.abs(rd.nextInt())%candidates.size();
			list=candidates.elementAt(ind)+1+Prefix;
		}//selection
		
		for(int i=0;i<choked.length;i++){
			choked[i]=true;
	    }
		if(PreferredNeighbour!=null){
			for(int i=0;i<PreferredNeighbour.length;i++){
				choked[PreferredNeighbour[i]%Prefix-1]=false;
			}
		}
		if(list!=-1){
			choked[list%Prefix-1]=false;
		}
		
		try{
			logfile.write(logs.logmsg(2, SelfID, list));
		}catch(Exception e){	
		}
		return list;
	}
	
	public static byte[] getPiece(int index){
		int size;
		if(index!=Pieces-1){
			size=PieceSize;
		}else{
			size=FileSize-(Pieces-1)*PieceSize;
		}
		int start=index*PieceSize;
		byte[] getdata=new byte[size];
                
		System.arraycopy(data,start,getdata,0,size);
System.out.println(getdata[0]);
		return getdata;
	}
	
	synchronized public static void writePiece(int index,byte[] getdata){
		Peers.elementAt(SelfID%Prefix-1).set(index);
		
		//data download completed, update log
		if(Peers.elementAt(SelfID%Prefix-1).gotAll()==true){
			try{
				logfile.write(logs.logmsg(9, SelfID));
			}catch(Exception e){
			}
		}
		
		int size=getdata.length;
		int start=index*PieceSize;
		System.arraycopy(getdata, 0, data, start, size);
	}
		
}
