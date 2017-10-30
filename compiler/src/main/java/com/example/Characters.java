package com.example;

/**
 * 通用字符集定义
 */
public class Characters
{
	
	static int CHAR=-1;
	static int NUM=-2;
	static int SPACE=-3;
	static int NO_NUM=-4;
	static int NO_CHAR=-5;
	static int NO_NUMCHAR=-6;
	static int NO_SPACE=-7;
	
	private Characters(){}

    static boolean isSpaceChar(int c)
    {
        return Character.isSpaceChar(c)||c==10||c==9;		//space enter tab
    }
}
