
import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.lang.Exception;
import java.util.logging.*;


public class process extends Thread{
	public Socket processSocket;
	public int OtherID;
	public boolean exit=false;
	public process(Socket temp){
		processSocket=temp;
		OtherID=-1;
	}
	
	public process(Socket temp,int id){
		processSocket=temp;
		OtherID=id;
	}
	
	String HandshakeHead="Bittorent2015";
	String HandshakeMessage=HandshakeHead+"0000000000"+peerProcess.SelfID+"\n";
	DataOutputStream outToOther;
	BufferedReader inFromOther;
	InputStream pieceReader;
	String checkheadermessage;
	String checkheader;
	String checkID;
	String unchokedMessage="1%%1%%-1\n";
	String chokedMessage="1%%0%%-1\n";
	String exitMessage="1%%8%%-1\n";
	String notexitMessage="1%%9%%-1\n";
	String chclearMessage="1%%10%%-1\n";
	BufferedReader MinFromOther;
	String MsFromOther;
	String[] RevMessage;
	int mslen=0;
	int msType=1;
	String msPayload;
	int intrpid=-1;
	String haveMessage;
	String mybitfield;
	String piecemessage;
	
	public void run(){
		try{
			inFromOther=new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
			pieceReader=processSocket.getInputStream();
			outToOther=new DataOutputStream(processSocket.getOutputStream());
		}catch(IOException ex){
			Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		//TCP handshake start, send handshake message to other
		if(OtherID!=-1){
			try{
				outToOther.writeBytes(HandshakeMessage);
			}catch(IOException ex){
				Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
			}
			try{
				while(inFromOther.ready()==false){
					Thread.sleep(5);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				checkheadermessage=inFromOther.readLine()+"";
			}catch(IOException ex){
				Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
			}
			checkheader=checkheadermessage.substring(0,13);
			OtherID=Integer.parseInt(checkheadermessage.substring(23, 27));
		}else{
			try{
				while(inFromOther.ready()==false){
					Thread.sleep(5);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				checkheadermessage=inFromOther.readLine()+"";
			}catch(IOException ex){
				Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
			}
			checkheader=checkheadermessage.substring(0, 13);
			OtherID=Integer.parseInt(checkheadermessage.substring(23, 27));
			//send ackhandshake message
			if(checkheader.equals(HandshakeHead)){
				try{
					outToOther.writeBytes(HandshakeMessage);
				}catch(IOException ex){
					Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
		//TCP handshake ends
		
		//File transfer process
		try{
			while(exit==false && (peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).gotAll()==false || peerProcess.Peers.elementAt(OtherID%1000-1).gotAll()==false)){
	            //not exit
				
				try{
					outToOther.writeBytes(notexitMessage);
					System.out.println("Peer" + peerProcess.SelfID+" sent not exit message to peer " +OtherID+".");
				}catch(Exception e){
					e.printStackTrace();
				}
				try{
					while(inFromOther.ready()==false){
						Thread.sleep(5);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				try{
					MsFromOther=inFromOther.readLine()+"";
					
				}catch(IOException ex){
					Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
				}
				RevMessage=MsFromOther.split("%%");
				mslen=Integer.parseInt(RevMessage[0]);
				msType=Integer.parseInt(RevMessage[1]);
				msPayload=RevMessage[2];
				if(msType==8){
					//receive exit message
					System.out.println("Peer "+peerProcess.SelfID+" received exit message from peer "+OtherID+".");
					exit=true;
					break;
				}else if(msType!=9){
					System.out.println("Expection exit/not exit message,received message type "+msType+".");
				}else{
					System.out.println("Peer "+peerProcess.SelfID+" received not exit message from peer "+OtherID+".");
				}
				
				//exchange bitField
				try{
					mybitfield=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).getBitfield();
					haveMessage=mybitfield.length()+"%%"+4+"%%"+mybitfield+"\n";
					outToOther.writeBytes(haveMessage);
					System.out.println("Peer "+peerProcess.SelfID+" set bitfield message to peer "+OtherID+".");
				}catch(Exception e){
					e.printStackTrace();
				}
				try{
					while(inFromOther.ready()==false){
						Thread.sleep(5);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				try{
					MsFromOther=inFromOther.readLine()+"";
				}catch(IOException ex){
					Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
				}
				RevMessage=MsFromOther.split("%%");
				mslen=Integer.parseInt(RevMessage[0]);
				msType=Integer.parseInt(RevMessage[1]);
				msPayload=RevMessage[2];
				if(msType==4){
					//received bitfield from other party
					System.out.println("Peer "+peerProcess.SelfID+" received bitfield message from peer "+OtherID+".");
					peerProcess.Peers.elementAt(OtherID%1000-1).setBitfield(msPayload);
				}else if(msType==8){
					//received exit message
					System.out.println("Peer "+peerProcess.SelfID+" received exit message from peer "+OtherID+".");
					exit=true;
					break;
				}else{
					System.out.println("Expection bitfield message, received message type "+msType+".");
				}
				//bitfield message received
				
				//check chock or unchock
				if(peerProcess.choked[OtherID%1000-1]==false){
					try{
						//if is unchocked, send the unchock message
						outToOther.writeBytes(unchokedMessage);
					}catch(IOException ex){
						Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
					}
					//send bitfield to others if unchoked
					mybitfield=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).getBitfield();
					haveMessage=mybitfield.length()+"%%"+4+"%%"+mybitfield+"\n";
					try{
						//send the have message
						outToOther.writeBytes(haveMessage);
					}catch(IOException ex){
						Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
					}
					try{
						while(inFromOther.ready()==false){
							Thread.sleep(5);
						}
					}catch(Exception e){
						e.printStackTrace();
					}
					try{
						MsFromOther=inFromOther.readLine();
					}catch(IOException ex){
						Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
					}
					RevMessage=MsFromOther.split("%%");
					mslen=Integer.parseInt(RevMessage[0]);
					msType=Integer.parseInt(RevMessage[1]);
					msPayload=RevMessage[2];
					if(msType==1){
						//if receive unchoke message
						//case 1: the other party and self unchoked
						try{
							peerProcess.logfile.write(logs.logmsg(3, peerProcess.SelfID,OtherID));
						}catch(IOException ex){
							Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
						}
						try{
							while(inFromOther.ready()==false){
								Thread.sleep(5);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							MsFromOther=inFromOther.readLine();
						}catch(IOException ex){
							Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
						}
						RevMessage=MsFromOther.split("%%");
						mslen=Integer.parseInt(RevMessage[0]);
						msType=Integer.parseInt(RevMessage[1]);
						msPayload=RevMessage[2];
						if(msType==4){
							//receive have message
							peerProcess.Peers.elementAt(OtherID%1000-1).setBitfield(msPayload);
							intrpid=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).interest(OtherID);
							try{
								peerProcess.logfile.write(logs.logmsg(5, peerProcess.SelfID, OtherID,intrpid));	
							}catch(IOException ex){
								Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
							}
							if(intrpid==-1){
								//send not interested message
								try{
									outToOther.writeBytes(1+"%%"+3+"%%"+"-1\n");
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								//determine receive is interested or not interested
								try{
									while(inFromOther.ready()==false){
										Thread.sleep(5);
									}
								}catch(Exception e){
									e.printStackTrace();
								}
								try{
									MsFromOther=inFromOther.readLine()+"";
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								RevMessage=MsFromOther.split("%%");
								mslen=Integer.parseInt(RevMessage[0]);
								msType=Integer.parseInt(RevMessage[1]);
								msPayload=RevMessage[2];
								if(msType==2){
									//if receive interested
									try{
										peerProcess.logfile.write(logs.logmsg(6, peerProcess.SelfID,OtherID));
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
									byte[] piececontent=peerProcess.getPiece(Integer.parseInt(msPayload));
									piecemessage=msPayload+"%%"+7+"%%"+piececontent.length+"\n";
									try{
										//receive interest message, send piece message
										outToOther.writeBytes(piecemessage);
										while(inFromOther.ready()==false){
											Thread.sleep(5);
										}
										try{
											MsFromOther=inFromOther.readLine();
										}catch(Exception e){}
										RevMessage=MsFromOther.split("%%");
										mslen=Integer.parseInt(RevMessage[0]);
										msType=Integer.parseInt(RevMessage[1]);
										msPayload=RevMessage[2];
										if(msType==10){
											//receive clear message,will send content message
											outToOther.write(piececontent);
											while(inFromOther.ready()==false){}
											try{
												MsFromOther=inFromOther.readLine();
											}catch(Exception e){}
											RevMessage=MsFromOther.split("%%");
											mslen=Integer.parseInt(RevMessage[0]);
											msType=Integer.parseInt(RevMessage[1]);
											msPayload=RevMessage[2];
											if(msType!=10){
												System.out.println("Expecting channel clear message,receive message type "+msType);
											}
											continue;
										}else{
											System.out.println("Expecting channel clear message,recieve message type "+msType);
										}
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
								}else if(msType==3){
									//if receive not interested
									try{
										//not interested
										peerProcess.logfile.write(logs.logmsg(7, peerProcess.SelfID,OtherID));
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
								}
								//receive interested or not interested determined
							}else{
								//send interested message
								try{
									outToOther.writeBytes(1+"%%"+2+"%%"+intrpid+"\n");
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								//determine receive message is interested or not interested
								try{
									while(inFromOther.ready()==false){
										Thread.sleep(5);
									}
								}catch(Exception e){
									e.printStackTrace();
								}
								try{
									MsFromOther=inFromOther.readLine()+"";
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								RevMessage=MsFromOther.split("%%");
								mslen=Integer.parseInt(RevMessage[0]);
								msType=Integer.parseInt(RevMessage[1]);
								msPayload=RevMessage[2];
								if(msType==2){
									//if receive interested
									byte[] piececontent=peerProcess.getPiece(Integer.parseInt(msPayload));
									piecemessage=msPayload+"%%"+7+"%%"+piececontent.length+"\n";
									try{
										//receive interest message,send piece message
										try{
											peerProcess.logfile.write(logs.logmsg(6, peerProcess.SelfID,OtherID));
										}catch(IOException ex){
											Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
										}
										outToOther.writeBytes(piecemessage);
										while(inFromOther.ready()==false){
											Thread.sleep(5);
										}
										try{
											MsFromOther=inFromOther.readLine();
										}catch(Exception e){
											e.printStackTrace();
										}
										RevMessage=MsFromOther.split("%%");
										mslen=Integer.parseInt(RevMessage[0]);
										int indexFromOther=mslen;
										msType=Integer.parseInt(RevMessage[1]);
										msPayload=RevMessage[2];
										int loadsize=Integer.parseInt(msPayload);
										byte[] mspiececon=new byte[loadsize];
										if(msType==7){
											if(peerProcess.SelfID < OtherID){
												//seldid<otherid,send channel clear message
												outToOther.writeBytes(chclearMessage);
												//wait for data
												while(pieceReader.available()<=0){}
												//read piececontent
												int totallen=0;
												byte[] c=new byte[1];
												while(totallen<loadsize){
													try{
														pieceReader.read(c);
													}catch(Exception e){}
													mspiececon[totallen]=c[0];
													totallen++;
												}
												outToOther.write(piececontent);
												try{
													//piece
													peerProcess.writePiece(indexFromOther, mspiececon);
													peerProcess.logfile.write(logs.logmsg(8, peerProcess.SelfID,OtherID,intrpid,peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).HaveHowMany()));
												}catch(IOException ex){
													Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
												}
												//wait for other party to finish reading
												while(inFromOther.ready()==false){
													Thread.sleep(5);
												}
												try{
													MsFromOther=inFromOther.readLine();
												}catch(Exception e){
													e.printStackTrace();
												}
												RevMessage=MsFromOther.split("%%");
												mslen=Integer.parseInt(RevMessage[0]);
												msType=Integer.parseInt(RevMessage[1]);
												msPayload=RevMessage[2];
												if(msType!=10){
													System.out.println("Expection channel clear message, receive message type "+msType);
												}
												continue;
											}else{
												//selfid>otherid
												//wait for channel clear signal from the other party
												while(inFromOther.ready()==false){
													Thread.sleep(5);
												}
												try{
													MsFromOther=inFromOther.readLine();
												}catch(Exception e){
													e.printStackTrace();
												}
												RevMessage=MsFromOther.split("%%");
												mslen=Integer.parseInt(RevMessage[0]);
												msType=Integer.parseInt(RevMessage[1]);
												msPayload=RevMessage[2];
												if(msType==10){
													//got channel clear message
													outToOther.write(piececontent);
													//wait for the other party finish reading,wait for piececontent from the other party
													while(pieceReader.available() <=0){}
													//read piececontent
													int totallen=0;
													byte[] c=new byte[1];
													while(totallen<loadsize){
														try{
															pieceReader.read(c);
														}catch(Exception e){
															e.printStackTrace();
														}
														mspiececon[totallen]=c[0];
														totallen++;
													}
													outToOther.writeBytes(chclearMessage);
													try{
														//write piece
														peerProcess.writePiece(indexFromOther, mspiececon);
														peerProcess.logfile.write(logs.logmsg(8,peerProcess.SelfID,OtherID,  intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).HaveHowMany()));
													}catch(IOException ex){
														Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
													}
													continue;
												}else{
													System.out.println("Expecting channel clear message, receive message type "+msType);
												}
											}//selfid compare with otherid
										}else{
											System.out.println("Expecting piecemessage, receive message type "+msType);
										}//piecemessage received
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
								}else if(msType==3){
									//receive not interested
									try{
										//not interested
										peerProcess.logfile.write(logs.logmsg(7, peerProcess.SelfID,OtherID));
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
									try{
										//wait for piece
										MsFromOther=inFromOther.readLine()+"";
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
									RevMessage=MsFromOther.split("%%");
									mslen=Integer.parseInt(RevMessage[0]);
									int indexFromOther=mslen;
									msType=Integer.parseInt(RevMessage[1]);
									msPayload=RevMessage[2];
									int loadsize=Integer.parseInt(msPayload);
									byte[] mspiececon=new byte[loadsize];
									if(msType==7){
										outToOther.writeBytes(chclearMessage);
										while(pieceReader.available()<=0){}
										int totallen=0;
										byte[] c=new byte[1];
										while(totallen<loadsize){
											try{
												pieceReader.read(c);
											}catch(Exception e){
												e.printStackTrace();
											}
											mspiececon[totallen]=c[0];
											totallen++;
										}
										outToOther.writeBytes(chclearMessage);
										try{
											//get piece
											peerProcess.writePiece(indexFromOther, mspiececon);
											peerProcess.logfile.write(logs.logmsg(8, peerProcess.SelfID,OtherID,intrpid,peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).HaveHowMany()));
										}catch(IOException ex){
											Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
										}
										continue;
									}else{
										System.out.println("Expecting piece data, receive message type "+msType+".");
									}//receive piece data
								}//whether the other party is interested in self or not
							}//whether self is interested in the other party or not
						}//receive have message
					}else if(msType==0){
						//if receive choke message
						
						try{
							peerProcess.logfile.write(logs.logmsg(4, peerProcess.SelfID,OtherID));
						}catch(IOException ex){
							Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
						}
						
						try{
							while(inFromOther.ready()==false){
								Thread.sleep(5);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							MsFromOther=inFromOther.readLine();
						}catch(IOException ex){
							Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
						}
						RevMessage=MsFromOther.split("%%");
						mslen=Integer.parseInt(RevMessage[0]);
						msType=Integer.parseInt(RevMessage[1]);
						msPayload=RevMessage[2];
						if(msType==2){
							//if receive interested

							byte[] piececontent=peerProcess.getPiece(Integer.parseInt(msPayload));

							piecemessage=msPayload+"%%"+7+"%%"+piececontent.length+"\n";
                                                        
							try{
								//send piece message
								outToOther.writeBytes(piecemessage);
								while(inFromOther.ready()==false){
									Thread.sleep(5);
								}
								try{
									MsFromOther=inFromOther.readLine();
								}catch(Exception e){
									e.printStackTrace();
								}
								RevMessage=MsFromOther.split("%%");
								mslen=Integer.parseInt(RevMessage[0]);
								msType=Integer.parseInt(RevMessage[1]);
								msPayload=RevMessage[2];
								if(msType==10){
									//receive channel clear message, send content message
									outToOther.write(piececontent);
									while(inFromOther.ready()==false){
										Thread.sleep(5);
									}
									try{
										MsFromOther=inFromOther.readLine();
									}catch(Exception e){
										e.printStackTrace();
									}
									RevMessage=MsFromOther.split("%%");
									mslen=Integer.parseInt(RevMessage[0]);
									msType=Integer.parseInt(RevMessage[1]);
									msPayload=RevMessage[2];
									if(msType!=10){
										System.out.println("Expection channel clear message, receive message type "+msType+".");
									}
									continue;
								}else{
									System.out.println("Expection channel clear message, receive message type "+msType+".");
								}
								
							}catch(IOException ex){
								Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
							}
							try{
								peerProcess.logfile.write(logs.logmsg(6, peerProcess.SelfID,OtherID));
							}catch(IOException ex){
								Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
							}
						}else if(msType==3){
							try{
								//if receive not interest
								peerProcess.logfile.write(logs.logmsg(7, peerProcess.SelfID,OtherID));
							}catch(IOException ex){
								Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
							}
						}//whether recieved interest or not
					}else {continue;} //whether recieved choke or unchoke
				}else{//send chock message
					try{
						outToOther.writeBytes(chokedMessage);
					}catch(IOException ex){
						Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
					}
					//wait for choke/unchoke message from the other party
					try{
						while(inFromOther.ready()==false){
							Thread.sleep(5);
						}
					}catch(Exception e){}
					try{
						MsFromOther=inFromOther.readLine()+"";
					}catch(IOException ex){
						Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
					}
					RevMessage=MsFromOther.split("%%");
					mslen=Integer.parseInt(RevMessage[0]);
					msType=Integer.parseInt(RevMessage[1]);
					msPayload=RevMessage[2];
					if(msType==0){
						//receive choke message, both parties are chocked. Do nothing.
						try{
							peerProcess.logfile.write(logs.logmsg(4, peerProcess.SelfID,OtherID));
						}catch(Exception e){
							e.printStackTrace();
						}
						Thread.sleep(5);
						continue;
					}else if(msType==1){
						//receive unchoked message, wait for have message
						try{
							peerProcess.logfile.write(logs.logmsg(3, peerProcess.SelfID,OtherID));
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							while(inFromOther.ready()==false){
								Thread.sleep(5);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							MsFromOther=inFromOther.readLine()+"";
						}catch(IOException ex){
							Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
						}
						RevMessage=MsFromOther.split("%%");
						mslen=Integer.parseInt(RevMessage[0]);
						msType=Integer.parseInt(RevMessage[1]);
						msPayload=RevMessage[2];
						if(msType==4){
							//receive have message
							peerProcess.Peers.elementAt(OtherID%1000-1).setBitfield(msPayload);
							intrpid=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).interest(OtherID);
							try{
								peerProcess.logfile.write(logs.logmsg(5, peerProcess.SelfID,OtherID,intrpid));
							}catch(Exception e){
								e.printStackTrace();
							}
							if(intrpid==-1){
								//send not interested message
								try{
									outToOther.writeBytes(1+"%%"+3+"%%"+"-1\n");
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
							}else{
								//send interested message
								try{
									outToOther.writeBytes(1+"%%"+2+"%%"+intrpid+"\n");
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								//wait for data
								try{
									while(inFromOther.ready()==false){
										Thread.sleep(5);
									}
								}catch(Exception e){
									e.printStackTrace();
								}
								try{
									MsFromOther=inFromOther.readLine()+"";
								}catch(IOException ex){
									Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
								}
								RevMessage=MsFromOther.split("%%");
								mslen=Integer.parseInt(RevMessage[0]);
								int indexFromOther=mslen;
								msType=Integer.parseInt(RevMessage[1]);
								msPayload=RevMessage[2];
								int loadsize=Integer.parseInt(msPayload);
								byte[] mspiececon=new byte[loadsize];
								if(msType==7){
									//receive piecemessage, send clear message
									outToOther.writeBytes(chclearMessage);
									int totallen=0;
									byte [] c=new byte[1];
									while(totallen<loadsize){
										try{
											pieceReader.read(c);
										}catch(Exception e){
											e.printStackTrace();
										}
										mspiececon[totallen]=c[0];
										totallen++;
									}
									outToOther.writeBytes(chclearMessage);
									try{
										//write piece
										peerProcess.writePiece(indexFromOther, mspiececon);
										peerProcess.logfile.write(logs.logmsg(8, peerProcess.SelfID, OtherID, intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).HaveHowMany()));
									}catch(IOException ex){
										Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
									}
									continue;
								}else{
									System.out.println("Expecting piece data, receive message type "+msType);
								}
							}//if interest or not
						}else{
							System.out.println("Expecting have message, receive message type "+msType);
						}//have message
					}else{
						System.out.println("Expecting choked/unchoked message, receive message type "+msType);
					}//if choked or not
				}//end of check chock or unchock
			}// end of while exit==0
		}catch(Exception ite){}
		
		//send exit message to the other peer
		if(exit==false){
			//selfpeer initiated the exit process
			try{
				outToOther.writeBytes(exitMessage);
				System.out.println("Peer "+peerProcess.SelfID+" sent exit message to peer "+OtherID +".");
				try{
					Thread.sleep(500);
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(IOException ex){
				Logger.getLogger(process.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		try{
			processSocket.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		//close process socket
		
		System.out.println("The worker thread between peer "+peerProcess.SelfID+"and peer "+OtherID+"teminate.\n");
		peerProcess.Peers.elementAt(OtherID%1000-1).setAll();
		peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).setAll();
	}
	
	
}
