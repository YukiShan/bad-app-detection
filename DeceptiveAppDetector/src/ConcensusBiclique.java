import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

public class ConcensusBiclique {
	private ArrayList<String> nodeYList=new ArrayList<String>();
	private ArrayList<String> nodeXList=new ArrayList<String>();
	//<name,index>
	private HashMap<String, Integer> nodeYHashMap=new HashMap<String, Integer>();
	private HashMap<String, Integer> nodeXHashMap=new HashMap<String, Integer>();
	
	private ArrayList<BitSet> nodeYSetList=new ArrayList<BitSet>();
	private HashSet<BitSet> setList=new HashSet<BitSet>();
	private LinkedBlockingQueue<BitSet> queue=new LinkedBlockingQueue<BitSet>();
	
	/**
	 * nodeX->nodeY
	 * @param edges
	 */
	public ConcensusBiclique(ArrayList<String> edges){
		int xIndex=-1;
		int yIndex=-1;
		for(String str:edges){
			String []temp=str.split(" ");
			if(temp.length<2){
				continue;
			}
			//find xIndex
			if(nodeXHashMap.containsKey(temp[0])){
				xIndex=nodeXHashMap.get(temp[0]);
			}else{
				xIndex=nodeXList.size();
				nodeXList.add(temp[0]);
				nodeXHashMap.put(temp[0], xIndex);
			}
			//find yIndex
			if(nodeYHashMap.containsKey(temp[1])){
				yIndex=nodeYHashMap.get(temp[1]);
			}else{
				yIndex=nodeYList.size();
				nodeYList.add(temp[1]);
				nodeYHashMap.put(temp[1], yIndex);
			}
			BitSet bs=null;
			//create an edge set of each node Y
			if(nodeYSetList.size()<=yIndex){
				bs=new BitSet();
				bs.set(xIndex);
				nodeYSetList.add(bs);
			}else{
				bs=nodeYSetList.get(yIndex);
				bs.set(xIndex);
			}
			
		}
		setList.addAll(nodeYSetList);
		queue.addAll(nodeYSetList);
	}
	/**
	 * Find all the maximal bicliques
	 */
	public ArrayList<Biclique> findAllMaximalBiclique(){
		while(!queue.isEmpty()){
			BitSet bsX=queue.poll();
			BitSet bsY=this.getNeighborSet(bsX);
			BitSet nodeYRerverse=new BitSet();
			nodeYRerverse.set(0, this.nodeYList.size(), true);
			bsY.xor(nodeYRerverse);
			int trueIndex=-1;
			while(true){
				trueIndex++;
				trueIndex=bsY.nextSetBit(trueIndex);
				if(trueIndex==-1){
					break;
				}
				BitSet bsXTemp=(BitSet)nodeYSetList.get(trueIndex).clone();
				bsXTemp.and(bsX);
				if(!setList.contains(bsXTemp)&& bsXTemp.cardinality()>0){
					setList.add(bsXTemp);
					queue.add(bsXTemp);
				}
			}
		}
		ArrayList<Biclique> bicliqueList=new ArrayList<Biclique>();
		for(BitSet bsX: setList){
			BitSet bsY=this.getNeighborSet(bsX);
			bicliqueList.add(this.print(bsX, bsY));
		}
		return bicliqueList;
	}
	/**
	 * Get the neighbor set of bsX which is a subset of bsY
	 * @param bsX
	 * @return
	 */
	private BitSet getNeighborSet(BitSet bsX){
		BitSet bsY=new BitSet();
		for(int i=0;i<nodeYSetList.size();i++){
			BitSet bsTemp=(BitSet)nodeYSetList.get(i).clone();
			bsTemp.and(bsX);
			bsTemp.xor(bsX);
			if(bsTemp.cardinality()==0){
				bsY.set(i);
			}
		}
		return bsY;
	}
	/**
	 * Print out the bicliques 
	 * @param bsX
	 * @param bsY
	 */
	private Biclique print(BitSet bsX, BitSet bsY){
		int index=-1;
		Biclique biclique=new Biclique();
		biclique.setX=new ArrayList<String>();
		biclique.setY=new ArrayList<String>();
//		System.out.print("Y: ");
		while(true){
			index++;
			index=bsY.nextSetBit(index);
			if(index==-1){
				break;
			}
			biclique.setY.add(nodeYList.get(index));
//			System.out.print(nodeYList.get(index)+"\t");
		}
//		System.out.println();
//		System.out.print("X: ");
		index=-1;
		while(true){
			index++;
			index=bsX.nextSetBit(index);
			if(index==-1){
				break;
			}
//			System.out.print(nodeXList.get(index)+"\t");
			biclique.setX.add(nodeXList.get(index));
		}
//		System.out.println();
		return biclique;
	}
	public ArrayList<String> getNodeYList() {
		return nodeYList;
	}
	
	public HashSet<BitSet> getSetList() {
		return setList;
	}
	public ArrayList<String> getNodeXList() {
		return nodeXList;
	}
	public static void main(String []args){
		try{
			File file=new File("src/largeScaleEngine/input.txt");
			ArrayList<String> edgeList=new ArrayList<String>();
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String input=null;
			while((input=br.readLine())!=null){
				edgeList.add(input);
			}
			ConcensusBiclique concensusBiclique=new ConcensusBiclique(edgeList);
			concensusBiclique.findAllMaximalBiclique();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
