package com.googlecode.openwnn.legacy;

import java.util.ArrayList;

public class WnnSentence extends WnnWord
{
    public ArrayList<WnnClause> elements;
    
    public WnnSentence(String input, WnnClause clause)
    {
        this.id = clause.id;
        this.candidate = clause.candidate;
        this.stroke = input;
        this.frequency = clause.frequency;
        this.partOfSpeech = clause.partOfSpeech;
        this.attribute = clause.attribute;
        this.elements = new ArrayList<WnnClause>();
        this.elements.add(clause);
    }
    
    public WnnSentence(WnnSentence prev, WnnClause clause)
    {
        this.id = prev.id;
        this.candidate = prev.candidate + clause.candidate;
        this.stroke = prev.stroke + clause.stroke;
        this.frequency = prev.frequency + clause.frequency;
        this.partOfSpeech = new WnnPOS(prev.partOfSpeech.left, clause.partOfSpeech.right);
        this.attribute = prev.attribute;
        
        this.elements = new ArrayList<WnnClause>();
        this.elements.addAll(prev.elements);
        this.elements.add(clause);
    }
    
    public WnnSentence(WnnClause head, WnnSentence tail)
    {
        if (tail == null)
        {
            /* single clause */
            this.id = head.id;
            this.candidate = head.candidate;
            this.stroke = head.stroke;
            this.frequency = head.frequency;
            this.partOfSpeech = head.partOfSpeech;
            this.attribute = head.attribute;
            this.elements = new ArrayList<WnnClause>();
            this.elements.add(head);
        }
        else
        {
            /* consecutive clauses */
            this.id = head.id;
            this.candidate = head.candidate + tail.candidate;
            this.stroke = head.stroke + tail.stroke;
            this.frequency = head.frequency + tail.frequency;
            this.partOfSpeech = new WnnPOS(head.partOfSpeech.left, tail.partOfSpeech.right);
            this.attribute = 2;
            this.elements = new ArrayList<WnnClause>();
            this.elements.add(head);
            this.elements.addAll(tail.elements);
        }
    }
}
