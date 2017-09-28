import data.AppData;
/**
 * It is a maximum heap sort for sorting objects in terms of one class member.
 * @author Shanshan Li
 *
 */
public class HeapSort {
	class Node{
	  Node left=null;
	  Node right=null;
	  Node parent=null;
	  Object appData=null;//Object
	  int value;//member
	  public Node(int value, Object appData) {
	   this.value = value;
	   this.appData=appData;
	  }
	 }
	
	private Node root=null;
	
	public HeapSort(){
		
	}
	/**
	 * Insert an object for sorting.
	 * @param value - the value for sorting
	 * @param obj - the object contains the value
	 */
	public void insertObject(int value,Object obj){
		Node appNode=new Node(value,obj);
		if(root==null){
			root=appNode;
		}else{
			hsort(appNode,root);
		}
	}
	/**
	 * Clear all the cache of the heap sort tree. Just free the pointer to node
	 */
	public void clear(){
		this.clearHeapSort(root);
	}
	
	private void clearHeapSort(Node node){
		if(node==null){
			return;
		}
		if(node.left!=null){
			clearHeapSort(node.left);
		}
		if(node.right!=null){
			clearHeapSort(node.right);
		}
		node=null;
	}
	/**
	 * Delete root
	 * @return The object of the root
	 */
	public Object removeRoot(){
		if(root==null||root.value==-1){
			return null;
		}
		Node node=root;
		Object temp=node.appData;
		node.value=-1;
		adjustHeap(node,false);
		return temp;
	}
	private void hsort(Node node, Node tempRoot){
		if(tempRoot==null){
			tempRoot=node;
			return;
		}
		while(tempRoot.left!=null && tempRoot.right!=null){
			if(Math.random()>0.5){
				tempRoot=tempRoot.left;
			}else{
				tempRoot=tempRoot.right;
			}
		}
		if(tempRoot.left==null){
			tempRoot.left=node;
			node.parent=tempRoot;
		}else{
			tempRoot.right=node;
			node.parent=tempRoot;
		}
		adjustHeap(node.parent,true);
	}
	/**
	 * 
	 * @param node
	 * @param direction - true: up false: low
	 */
	private void adjustHeap(Node parent, boolean direction){
		Node temp=null;
		if(parent.left==null){
			temp=parent.right;
		}else if(parent.right==null){
			temp=parent.left;
		}else{
			if(parent.left.value>parent.right.value){
				temp=parent.left;
			}else{
				temp=parent.right;
			}
		}
		if(temp==null){
			return;
		}
		if(parent.value>=temp.value){
			return;
		}else{//parent>temp swap these two nodes
			swap(parent,temp);

			if(direction){//up
				if(parent.parent!=null){
					adjustHeap(parent.parent,direction);
				}
			}else{//down
				adjustHeap(temp,direction);
			}
		}
	}
	private void swap(Node parent,Node temp){
		int tempVal=parent.value;
		parent.value=temp.value;
		temp.value=tempVal;
		
		Object tempNode=parent.appData;
		parent.appData=temp.appData;
		temp.appData=tempNode;
	}
	
	/*
	 * Test code
	 public static void main(String []args){
		HeapSort hs=new HeapSort();
		AppData ad[]=new AppData[9];
		ad[0]=new AppData(""); ad[0].setRank(5);
		ad[1]=new AppData(""); ad[1].setRank(22);
		ad[2]=new AppData(""); ad[2].setRank(22);
		ad[3]=new AppData(""); ad[3].setRank(3);
		ad[4]=new AppData(""); ad[4].setRank(8);
		ad[5]=new AppData(""); ad[5].setRank(7);
		ad[6]=new AppData(""); ad[6].setRank(6);
		ad[7]=new AppData(""); ad[7].setRank(3);
		ad[8]=new AppData(""); ad[8].setRank(3);
		for(int i=0;i<9;i++){
			hs.insertObject(ad[i].getRank(),ad[i]);
		}
		for(int i=0;i<9;i++){
			System.out.println(((AppData)hs.removeRoot()).getRank());
		}
		hs.clear();
	}*/
}
