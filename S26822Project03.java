import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class S26822Project03
{
    private ArrayList<Layer> layers;
    public S26822Project03()
    {
        this.layers=new ArrayList<>();
        VRDS vrds=new VRDS();
        BTSLayer btsLayer=new BTSLayer(vrds);
        BSCLayer bscLayer=new BSCLayer(btsLayer);
        BTSLayer btsLayer1=new BTSLayer(bscLayer);
        VBDS vbds= new VBDS(btsLayer1,vrds);
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        vbds.stations.add(new VBD(vbds));
        this.layers.add(vbds);
        this.layers.add(btsLayer1);
        this.layers.add(bscLayer);
        this.layers.add(btsLayer);
        this.layers.add(vrds);
    }
    public void createBSCLayer()
    {
        this.layers.add(2,new BSCLayer(this.layers.get(2)));
        this.layers.get(1).next=this.layers.get(2);
    }
    public void removeBSCLayer(int num)
    {
       this.layers.get(num-1).next=this.layers.get(num+1);
       this.layers.get(num).extremePass();
       this.layers.remove(num);
    }
    public static void main(String[] args)
    {
        new S26822Project03();
    }
    class VBDS extends Layer
    {
        private VRDS vrds;
        VBDS(Layer next, VRDS vrds)
        {
            super(next);
            this.vrds=vrds;
            this.stations=new ArrayList<>();
            this.stations.add(new VBD(this));
        }
    }
    class VBD extends Station
    {
        private volatile boolean suspended;
        private final SMS sms;
        VBD(VBDS current)
        {
            super(current);
            this.suspended=false;
            Scanner scanner=new Scanner(System.in);
            System.out.println("Enter message");
            this.sms=new SMS(this.number,(int)(Math.random()*((VBDS)(this.current)).vrds.stations.size()+1),scanner.next());
            this.start();
        }
        public void setDelay(long delay)
        {
            this.delay=delay;
        }
        public void endVBD()
        {
            this.running=false;
        }
        public void suspendVBD()
        {
            this.suspended=true;
        }
        public void resumeVBD()
        {
            this.suspended=false;
            synchronized (this)
            {
                notify();
            }
        }
        @Override
        public void run()
        {
            while (running)
            {
                while (suspended)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                this.passTo(this.current.next.findMin(this.sms),this.sms);
                try
                {
                    Thread.sleep(this.delay);
                }
                catch (InterruptedException ignored){}
            }
        }
    }
    class SMS
    {
        private int sender;
        private int recipient;
        private String message;

        public void setRecipient(int recipient)
        {
            this.recipient = recipient;
        }
        SMS(int sender,int recipient, String message)
        {
            this.message=message;
            this.sender=sender;
            this.recipient=recipient;
        }
    }

    class BTSLayer extends Layer
    {
        public void createNew()
        {
            this.stations.add(new BTSL(this));
        }
        @Override
        public Station findMin(SMS sms) {
            Station s= super.findMin(sms);
            if(this.isPiled())this.createNew();
            return this.stations.get(this.stations.size()-1);
        }
        BTSLayer(Layer next)
        {
            super(next);
            this.stations=new ArrayList<>();
            this.stations.add(new BTSL(this));
        }
    }
    abstract class Layer
    {
         Layer next;
         ArrayList<Station> stations;
        Layer(Layer next)
        {
            this.next=next;
        }
        public void extremePass()
        {
            for(Station s :this.stations)
            {
                s.extremePass();
                s.running=false;
            }
        }
        public void removeStation(Station station)
        {
            this.stations.remove(station);
        }
        public Station findMin(SMS sms)
        {
            Station tmp=this.next.stations.get(0);
            for(int i=1;i<this.next.stations.size();i++)
            {
                if(tmp.record.size()>this.next.stations.get(i).record.size())
                    tmp=this.next.stations.get(i);
            }
            return tmp;
        }
        public boolean isPiled()
        {
            for(Station e: this.stations)
            {
                if(e.record.size()<5)
                    return false;
            }
            return true;
        }
    }
    class BTSL extends Station
    {
        BTSL(BTSLayer current)
        {
            super(current);
            this.start();
        }
    }
    class BSC extends Station
    {
        BSC(BSCLayer current)
        {
            super(current);
            this.start();
        }
    }
    class BSCLayer extends Layer
    {
        public void createNew()
        {
            this.stations.add(new BSC(this));
        }
        @Override
        public Station findMin(SMS sms) {
            Station s= super.findMin(sms);
            if(this.isPiled())this.createNew();
            return this.stations.get(this.stations.size()-1);
        }
        BSCLayer(Layer next)
        {
            super(next);
            this.stations=new ArrayList<>();
            this.stations.add(new BSC(this));
        }
    }
    abstract class Station extends Thread
    {
        final int number;
         long delay;
         Layer current;
         volatile boolean running=true;
         ConcurrentHashMap<SMS, Long> record;
        Station(Layer current)
        {
            this.current=current;
            this.number=this.current.stations.size()+1;
            this.record=new ConcurrentHashMap<>();
            this.delay=(long) (Math.random()*16000)+5000;
        }
        @Override
        public void run()
        {
            while (running)
            {
                while (this.record.size()>0)
                {
                    for(Map.Entry<SMS,Long> e: this.record.entrySet())
                        {
                            long currentTime=System.currentTimeMillis();
                            if(currentTime-e.getValue() >=this.delay)
                            {
                                this.passTo(this.current.next.findMin(e.getKey()),e.getKey());
                            }
                        }

                }
            }
            this.current.removeStation(this);
        }
        public void extremePass()
        {
            for(Map.Entry<SMS,Long> e: this.record.entrySet())
            {
                this.passTo(this.current.next.findMin(e.getKey()),e.getKey());
            }
        }
        public void removeData(SMS sms)
        {
            this.record.remove(sms);
        }
        public void addData(SMS sms, long timestamp)
        {
            this.record.put(sms,timestamp);
        }
        public void passTo(VRD vrd,SMS sms)
        {
            if(vrd.record.containsKey(sms)) vrd.record.put(sms,vrd.record.get(sms)+1);
            else vrd.record.put(sms,1L);
            System.out.println("VRD number "+vrd.number+" received SMS");
        }
        synchronized void passTo(Station station,SMS sms)
    {
        station.addData(sms,System.currentTimeMillis());
            System.out.println("Station number "+station.number+" "+station.current.getClass()+" "+(station.current.next!=null?station.current.next.getClass():"")+" received SMS "+sms.message);
        this.removeData(sms);
    }
    }
    class VRDS extends Layer
        {
            VRDS()
            {
                super(null);
                this.stations =new ArrayList<>();
                this.stations.add(new VRD(this));
            }
            @Override
            public Station findMin(SMS sms)
            {
                return this.stations.get(sms.recipient-1);
            }
        }
    class VRD extends Station
    {
        private volatile boolean delition=false;
        @Override
        public void addData(SMS sms, long timestamp)
        {
            super.addData(sms, timestamp);
        }

        @Override
        public void run()
        {
            while (this.running)
            {
                while (this.delition)
                {
                    for(Map.Entry<SMS,Long> e: this.record.entrySet())
                    {
                        this.record.put(e.getKey(),0l);
                    }
                    try
                    {
                        Thread.sleep(10000);
                    }
                    catch (InterruptedException ignored){}
                }
            }
        }
        VRD(VRDS current)
        {
            super(current);
            this.start();
        }
    }
}
 class Try extends JFrame
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(Try::new);
    }
    public Try()
    {
        this.setSize(1440,800);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());
        VBDLayer left=new VBDLayer();
        this.getContentPane().add(left,BorderLayout.LINE_START);
        VRDLayer right=new VRDLayer();
        this.getContentPane().add(right,BorderLayout.LINE_END);
        Middle middle=new Middle();
        this.getContentPane().add(middle,BorderLayout.CENTER);
    }
    abstract class Layer extends JPanel
    {
        ArrayList<JPanel> stations;
        JScrollPane pane;
        JPanel panel;
        Layer()
        {
            this.setLayout(new BorderLayout());
            this.setPreferredSize(new Dimension(240,800));
            this.panel=new JPanel();
            this.panel.setLayout(new BoxLayout(this.panel,BoxLayout.Y_AXIS));
            this.pane=new JScrollPane(this.panel);
            this.stations=new ArrayList<>();
            this.add(pane);
        }
        abstract void adding();
    }
    class VBDLayer extends Layer
    {
        private JButton button;
        VBDLayer()
        {
            super();
            this.stations.add(new VBD(this));
            this.panel.add(this.stations.get(0));
            this.button=new JButton("ADD VBD");
            this.button.addActionListener(e->adding());
            this.add(this.button,BorderLayout.SOUTH);
        }
        void adding()
        {
            VBD vbd=new VBD(this);
            this.stations.add(vbd);
            this.panel.add(vbd);
            this.panel.revalidate();
            this.panel.repaint();
        }
    }
    class Middle extends JPanel
    {
        Middle()
        {
            this.setLayout(new BorderLayout());
            this.setPreferredSize(new Dimension(400,800));
            BTSLayer BTS1=new BTSLayer();
            BTSLayer BTS2=new BTSLayer();
            this.add(BTS1,BorderLayout.WEST);
            this.add(BTS2,BorderLayout.EAST);
            BSCLAYERSLAYER bsclayerslayer=new BSCLAYERSLAYER();
            this.add(bsclayerslayer);
        }
    }
    class VRDLayer extends Layer
    {
        private JButton button;
        VRDLayer()
        {
            super();
            this.stations.add(new VRD(this));
            this.panel.add(this.stations.get(0));
            this.button=new JButton("ADD VRD");
            this.button.addActionListener(e->adding());
            this.add(this.button,BorderLayout.SOUTH);
        }
        void adding()
        {
            VRD vbd=new VRD(this);
            this.stations.add(vbd);
            this.panel.add(vbd);
            this.panel.revalidate();
            this.panel.repaint();
        }
    }
    class BTSLayer extends Layer
    {
        private JButton button;
        BTSLayer()
        {
            super();
            this.stations.add(new BTS(this));
            this.panel.add(this.stations.get(0));
            this.button=new JButton("ADD BTS");
            this.button.addActionListener(e->adding());
            this.add(button,BorderLayout.SOUTH);
        }
        void adding()
        {
            BTS vbd=new BTS(this);
            this.stations.add(vbd);
            this.panel.add(vbd);
            this.panel.revalidate();
            this.panel.repaint();
        }
    }
    class BSCLAYERSLAYER extends JPanel
    {
        private ArrayList<BSCLayer> layers;
        JScrollPane pane;
        JPanel panel;
        private JButton adding;
        private JButton removing;

        BSCLAYERSLAYER()
        {
            this.setLayout(new BorderLayout());
            this.setPreferredSize(new Dimension(150,800));
            this.panel=new JPanel();
            this.panel.setLayout(new BoxLayout(this.panel,BoxLayout.X_AXIS));
            this.pane=new JScrollPane(this.panel);
            this.layers=new ArrayList<>();
            this.layers.add(new BSCLayer());
            this.panel.add(this.layers.get(0));
            this.add(pane);
            this.adding =new JButton("ADD LAYER");
            this.adding.addActionListener(e->adding());
            this.removing=new JButton("REMOVE LAYER");
            this.removing.addActionListener(e->removing());
            this.add(this.adding,BorderLayout.NORTH);
            this.add(this.removing,BorderLayout.SOUTH);
        }
        void removing()
        {
            this.panel.remove(0);
            this.layers.remove(0);
            this.panel.revalidate();
            this.panel.repaint();
        }
        void adding()
        {
            BSCLayer vbd=new BSCLayer();
            this.layers.add(vbd);
            this.panel.add(vbd);
            this.panel.revalidate();
            this.panel.repaint();
        }
    }
    class BSCLayer extends Layer
    {
        private JButton button;
        BSCLayer()
        {
            super();
            this.setPreferredSize(new Dimension(107,693));
            this.stations.add(new BTS(this));
            this.panel.add(this.stations.get(0));

            this.button=new JButton("ADD BSC");
            this.button.addActionListener(e->adding());
            this.add(this.button, BorderLayout.SOUTH);
        }
        void adding()
        {
            BSC vbd=new BSC(this);
            this.stations.add(vbd);
            this.panel.add(vbd);
            this.panel.revalidate();
            this.panel.repaint();
        }
    }
    class BSC extends Station
    {
        BSC(Layer layer)
        {
            super(layer);
            this.label=new JLabel("SMTH",JLabel.CENTER);this.add(this.label);
        }
    }
    class BTS extends Station
    {
        BTS(Layer layer)
        {
            super(layer);
            this.label=new JLabel("SMTH",JLabel.CENTER);this.add(this.label);
        }
    }
    class VBD extends JPanel
    {
        private Layer layer;
        private JSlider slider;
        private JButton button;
        private JTextField field;
        private JComboBox<String> box;
        VBD(Layer layer)
        {
            this.layer=layer;
            this.setPreferredSize(new Dimension(100,150));
            DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
            comboBoxModel.addElement("WAITING");
            comboBoxModel.addElement("ACTIVE");
            this.box=new JComboBox<>(comboBoxModel);
            this.field=new JTextField("SMTHNEW");
            this.field.setEditable(false);
            this.button=new JButton("TERMINATE");
            this.button.addActionListener(e->removing());
            this.slider= new JSlider(5, 15);
            slider.setMajorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            this.add(this.field);
            this.add(this.slider);
            this.add(this.button);
            this.add(this.box);
        }
        void removing()
        {
            this.layer.panel.remove(this);
            this.layer.stations.remove(this);
            this.layer.panel.revalidate();
            this.layer.panel.repaint();
        }
    }
    class VRD extends JPanel
    {
        private Layer layer;
        private JButton button;
        private JLabel label;
        private JCheckBox box;
        VRD(Layer layer)
        {
            this.layer=layer;
            this.setPreferredSize(new Dimension(100,100));
            this.box=new JCheckBox("DELETION");
            this.label=new JLabel("0");
            this.button=new JButton("TERMINATE");
            this.button.addActionListener(e->removing());
            this.add(this.label);
            this.add(this.button);
            this.add(this.box);
        }
        void removing()
        {
            this.layer.panel.remove(this);
            this.layer.stations.remove(this);
            this.layer.panel.revalidate();
            this.layer.panel.repaint();
        }
    }
    abstract class Station extends JPanel
    {
        Layer layer;
        JLabel label;
        JButton button;
        Station(Layer layer)
        {
            this.layer=layer;
            this.setPreferredSize(new Dimension(80,40));
            this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            this.setBackground(Color.yellow);
            this.button=new JButton("TERMINATE");
            this.button.addActionListener(e->removing());
            this.add(this.button);
        }
        void removing()
        {
            this.layer.panel.remove(this);
            this.layer.stations.remove(this);
            this.layer.panel.revalidate();
            this.layer.panel.repaint();
        }
    }
}