package com.klix.backend.model.treeview;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
@Setter
@Getter
public class Node
{
    private String nodeId;  // node id
    private String pid;     // parent id
    private String linkId;  // id used in url links / id of object of this node
    private String text;
    private String info;

    private String edit_url;
    private String add_url;

    private boolean deletable;
    private boolean childAddable;
    private boolean hasUser;
    private String create_user_url;


    /**
     * 
     */
    public Node(String nodeId, String pId, String text, String info)
    {
        this(nodeId, pId, text, info, "", "",  true, true);
    }


    /**
     * 
     */
    public Node(String nodeId, String pId, String text, String info, String edit_url, String add_url)
    {
        this(nodeId, pId, text, info, edit_url, add_url,  true, true);
    }


    /**
     * 
     */
    public Node(String nodeId, String pId, String text, String info, String edit_url, String add_url, boolean deletable, boolean childAddable)
    {
        this.nodeId = nodeId;
        this.pid = pId;
        this.text = text;
        this.info = info;
        this.edit_url = edit_url;
        this.add_url = add_url;
        this.deletable = deletable;
        this.childAddable = childAddable;
    }


    /**
     * 
     */
    public String toString(){
        return String.format("Node {id: %s, pid: %s, text: %s, href: %s, edit url: %s, add url: %s, deletable: %b}",
                             this.nodeId,
                             this.pid,
                             this.text,
                             this.info,
                             this.edit_url,
                             this.add_url,
                             this.deletable);
    }
}