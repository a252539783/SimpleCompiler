package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 file format:
 _G
 StatusNumber,Relation(Number),NextStatus;
 ....

 _K
 K1 K2...

 _T
 StatusNumber:Description

 1:loadDict
 2:loadSource
 3:getOne
 ...
 */
public class DictGraph
{

	
	
	private static DictGraph mInstance=new DictGraph();

    /**
     * For all nodes we loaded,first should build a map for searching(the variable mLoaded),
	 * and need a map for the nodes in the file may not be ordered and should index them.
	 */
	private Node mLoaded=new Node(false, 0);
	private Map<Integer,Node> mAll=new HashMap();
	private Map<Integer, String> mType=new HashMap<>();		//id:description

	private int last =-1;
	private int mKeyIndex=Integer.MIN_VALUE;

	private BufferedReader mCurrent;

	private DictGraph()
	{
		mAll.put(0, mLoaded);
	}

    /**
	 * @return next dict scan from source file
	 */
	public DictItem getOne()
	{
		Node node=mLoaded;
		int c=0;
		int failTimes=0;
		StringBuilder builder=new StringBuilder();

		for (int i=0;;i++)
		{
			try {
				/**
				 * meeting terminal and having scanned 2 or more characters,for the last character
				 * we scan may not be contained by the result we will get,so save it here and use it
				 * in next loop.Otherwise not.
				 */
				if (last!=-1)
				{
					c=(char)last;
					last=-1;
				}else
				{
					c=(char)mCurrent.read();
				}

				if (c==13||c==10)
					continue;

				if ((c==-1||c==65535)&&i==0)			//eof
					return new DictItem("error",-1,"reach the end of source");
				//p("read:"+c);
			} catch (IOException e) {
				failTimes++;

				if (failTimes>=5) {		//giving up when encounter 5 IOException
					return new DictItem("error",-1,e.toString());
				}
				continue;
			}

			node=node.get(c);
			if (node==null)
				return new DictItem("error",-1,"undefined dict");

			if (node.termi)
			{
				if (i!=0)
					last=c;			//save it
				else {
					last = -1;

					/**
					 * meets a terminal status when scanning first char,write it
					 */
					builder.append((char)c);
				}
				break;
			}

			builder.append((char)c);				//write all
		}

		return new DictItem(mType.get(node.index),node.index,builder.toString());
	}

	/**
	 * load the source
	 *
	 * @return true when succeed or false if the source file not found
	 */
	public boolean loadSource(String path)
	{
		try {
			mCurrent=new BufferedReader(new FileReader(new File(path)));
		} catch (FileNotFoundException e) {
			return false;
		}

		return true;
	}

	/**
	 * load dict file
	 *
	 * @param path path
	 * @return load successfully
	 */
	public boolean loadDict(String path)
	{
		try
		{
			BufferedReader r=new BufferedReader(new FileReader(new File(path)));
			String s;
			int stage=-1;
			while (true)
			{
				try {
					s=r.readLine();
					if (s==null)
						return true;
					if (s.contains("_G"))
					{
						stage=0;
						continue;
					}else if (s.contains("_K"))
					{
						stage=1;
						continue;
					}else if (s.contains("_T"))
					{
						stage=2;
						continue;
					}
					
					if (s.trim().length()==0)
						continue;
				} catch (IOException e) {
					return false;
				}
				
				
				switch (stage)
				{
				case 0:			//node

						String []c=s.split(",");
						int num1=Integer.parseInt(c[0].trim());
						int relation=0;
						if (c.length!=3)
						{
							relation=',';
						}else
						{
							if (c[1].contains("'"))
								relation=c[1].charAt(1);
							else
								relation=Integer.parseInt(c[1].trim());
						}

						c[c.length-1]=c[c.length-1].trim();
						int len=c[c.length-1].indexOf(';');
						int num2;
					/**
                     * ';' means terminal.
					 */
						if (len!=-1)
							num2=Integer.parseInt(c[c.length-1].substring(0, len));
						else
							num2=Integer.parseInt(c[c.length-1]);

						Node n1=mAll.get(num1);
						Node n2=mAll.get(num2);
						
						if (n1==null)
						{
							n1=new Node(false, num1);
							mAll.put(num1,n1);
						}
						
						if (n2==null)
						{
							if (len!=-1)
							{
								n2=new Node(true, num2);
							}else
							{
								n2=new Node(false, num2);
							}
							mAll.put(num2,n2);
						}
						
						n1.add(relation, n2);
					break;
				case 1:			//key  add all keys to Graph for avoid reading twice
					Node n=mLoaded;
					Node iden=mLoaded.nexts.get(Characters.CHAR);
					s=s.trim();
					for (int i=0;i<s.length();i++)
					{
						char m=s.charAt(i);
						Node cn=n.nexts.get((int)m);
						Set<Map.Entry<Integer,Node>> set= iden.nexts.entrySet();
						if (cn==null)
						{
							Iterator<Map.Entry<Integer,Node>> it=set.iterator();
							cn=new Node(false,Integer.MIN_VALUE);		//here the index for non-terminal has no sense
							while (it.hasNext())		//make the node point at the identifier node
							{
								Map.Entry<Integer,Node> e=it.next();
								cn.add(e.getKey(),e.getValue());
							}
							n.add(m,cn);
						}

						n=cn;
					}
					Node cn=new Node(true,mKeyIndex++);
					mType.put(mKeyIndex-1,"KEY");		//make every key be a type individually.
					n.add(Characters.NO_NUMCHAR,cn);
					break;
				case 2:			//type
					String[] cc=s.split(":");
					int index=Integer.parseInt(cc[0]);
					if (cc.length==2)
						mType.put(index, cc[1].trim());
					else		//meets ':'
					{
						mType.put(index, ":");
					}
					break;
				}
			}
		}
		catch (FileNotFoundException e)
		{
			return false;
		}
	}

	/**
	 * singleton
	 *
	 * @return the DictGraph's instance
	 */
	public static DictGraph get()
	{
		return mInstance;
	}

	private class Node
	{
		Map<Integer,Node> nexts;
		boolean termi=false;
		int index;
		
		Node(boolean t,int i)
		{
			termi=t;
			index=i;
		}
		
		Node add(int c,Node x)
		{
			if(nexts==null)
			{
				nexts=new HashMap();
			}
			
			nexts.put(c,x);
			
			return x;
		}

		/**
         * get the next node
		 *
		 * @param c  char
		 * @return   node
		 */
		Node get(int c)
		{
			Node x=nexts.get(c);		//key first
			if (x!=null)
				return x;

			if (Characters.isSpaceChar(c))
			{
				x=nexts.get(Characters.SPACE);

				if (x==null) {
					x = nexts.get(Characters.NO_NUM);
				}
				else{
					return x;
				}

				if (x==null) {
					x = nexts.get(Characters.NO_CHAR);
				}else
				{
					return x;
				}

				if (x==null)
				{
					x=nexts.get(Characters.NO_NUMCHAR);
				}else
				{
					return x;
				}

				if (x!=null) {
					return x;
				}
			}

			x=nexts.get(Characters.NO_SPACE);
			if (x!=null) {
				return x;
			}


			if (Character.isAlphabetic(c))		//char or no_num
			{
				x=nexts.get(Characters.CHAR);

				if (x==null)
				{
					x=nexts.get(Characters.NO_NUM);
				}else
				{
					return x;
				}



				if (x!=null)
				{
					return x;
				}
			}

			if (Character.isDigit(c))		//num or no_char
			{
				x= nexts.get(Characters.NUM);

				if (x==null)
				{
					x=nexts.get(Characters.NO_CHAR);
				}else
				{
					return x;
				}

				if (x!=null)
				{
					return x;
				}
			}

			//undefined char
			x=nexts.get(Characters.NO_NUMCHAR);
			if (x==null)
			{
				x=nexts.get(Characters.NO_NUM);
			}else
			{
				return x;
			}

			if (x==null)
			{
				x=nexts.get(Characters.NO_CHAR);
			}else
			{
				return x;
			}

			if (x==null)
			{
				x=nexts.get(Characters.NO_SPACE);
			}else
			{
				return x;
			}

			return x;
		}
	}

	public static class DictItem
	{
		public String classDesc;
		public int classId;
		public String originValue;

		public DictItem(String desc,int id,String value)
		{
			classDesc=desc;
			classId=id;
			originValue=value;
		}

		@Override
		public String toString() {
			return String.format("%-15s(%-15d):%-15s",classDesc,classId,originValue);
		}
	}
}
