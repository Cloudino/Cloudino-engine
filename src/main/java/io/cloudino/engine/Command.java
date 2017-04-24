/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

/**
 *
 * @author javiersolis
 */
public class Command
{
    public byte type=0;                //0=message, 1=log, 2=jscmd
    public byte topic[];
    public byte msg[];
}